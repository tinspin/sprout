package se.rupy.sprout;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.*;
import java.util.*;

import se.rupy.memory.*;
import se.rupy.pool.*;

/**
 * The node is the atomic persistence object.
 * @author Marc
 */
public class Node extends NodeBean implements Type {
	public static HashMap cache = new HashMap();

	public final static byte PARENT = 1 << 0;
	public final static byte CHILD = 1 << 1;
	public final static byte META = 1 << 2;
	public final static byte POLL = 1 << 3;

	static Format time = new SimpleDateFormat("d'&nbsp;'MMM'&nbsp;'H:mm");
	static Format date = new SimpleDateFormat("yy/MM/dd");

	private LinkBean link;
	private MetaBean meta;

	private boolean done;

	/**
	 * The node type should be a bit identifiable integer
	 * this limits the number of node types to 32.
	 * @param type
	 */
	public Node(int type) {
		this();
		setType(type);
	}

	protected Node(int type, long id) {
		this(type);
		setId(id);
	}

	protected Node() {
		link = new LinkBean();
		meta = new MetaBean();
	}

	protected boolean done() {
		return done;
	}

	protected void done(boolean done) {
		this.done = done;
	}

	/**
	 * Add child node. Inserts the child node automatically.
	 * @param node
	 * @throws SQLException
	 */
	public void add(Node node) throws SQLException {
		if(node == null) {
			throw new NullPointerException("Can't add node.");
		}

		if(id > 0 && (node.getId() == 0 || child(node.getId()) == null)) {
			update(node);
		}

		link.add(0, node);
	}

	/**
	 * Remove the child node.
	 * @param node
	 * @throws SQLException
	 */
	public void remove(Node node) throws SQLException {
		link.setParent(this);
		link.setChild(node);
		link.setType(getType() | node.getType());

		Sprout.update(Base.DELETE, link);
	}

	/**
	 * Add meta-data.
	 * @param type
	 * @param value
	 * @throws SQLException
	 */
	public void add(short type, String value) throws SQLException {
		Data data = new Data();
		data.setType(type);
		data.setValue(value);
		add(data);
	}

	/**
	 * Add meta-data.
	 * @param data
	 * @throws SQLException
	 */
	public void add(Data data) throws SQLException {
		if(data == null) {
			throw new NullPointerException("Can't add data.");
		}

		meta();

		Data old = meta(data.getType());
		Data cache = Data.cache(type, data.getValue());

		if(old != null) {
			if(old.getValue().equals(data.getValue())) {
				data.setId(old.getId());
				data.done(true);
			}
			else {
				meta.remove(old);

				if(cache != null && data.getId() == cache.getId()) {
					remove(old);
					meta(Base.INSERT, data, Sprout.connection(false));
				}
				else {
					data.setId(old.getId()); // TODO: Should this delete and insert? Nope, update() should be called?
				}
			}
		}
		else if(id > 0 && cache != null && data.getId() == cache.getId()) {
			meta(Base.INSERT, data, Sprout.connection(false));
		}

		if(id > 0 && data.getId() == 0) {
			meta(Base.INSERT, data, Sprout.connection(false));
		}

		meta.add(data);
	}

	/**
	 * Remove the meta-data.
	 * @param data
	 * @throws SQLException
	 */
	public void remove(Data data) throws SQLException {
		// TODO: remove data itself?

		meta.setNode(this);
		meta.setData(data);
		meta.setType(data.getType());

		Sprout.update(Base.DELETE, meta);
	}

	/**
	 * Deletes the node, it's meta-data, parent and/or child relations.
	 * Make sure all cached data is in the cache before you execute 
	 * this, otherwise this method might delete meta-data in use by 
	 * other nodes!
	 * @param what {@link #PARENT}, {@link #CHILD} and/or {@link #META}
	 * @return
	 * @throws SQLException
	 */
	public boolean delete(byte what) throws SQLException {
		if(id == 0) {
			throw new NullPointerException("Can't delete node.");
		}

		boolean success = false;
		Connection connection = Sprout.connection(true);

		try {
			if((what & PARENT) == PARENT) {
				Sprout.find("DELETE FROM link WHERE parent = " + id, connection);
			}

			if((what & CHILD) == CHILD) {
				Sprout.find("DELETE FROM link WHERE child = " + id, connection);
			}

			if((what & META) == META) {
				meta();

				Iterator it = meta.iterator();

				while(it.hasNext()) {
					Data data = (Data) it.next();
					Data cache = Data.cache(type, data.getValue());

					if(cache != null && data.getId() == cache.getId()) {
						System.out.println("Data cache for '" + cache.getValue() + "' found. (" + type + ")");
					}
					else {
						Sprout.update(Base.DELETE, data, connection);
					}
				}

				Sprout.find("DELETE FROM meta WHERE node = " + getId(), connection);
			}

			if((what & POLL) == POLL) {
				Sprout.find("DELETE FROM poll WHERE node = " + id, connection);
			}

			if((what & PARENT) == PARENT || 
					(what & CHILD) == CHILD || 
					(what & META) == META || 
					(what & POLL) == POLL) {
				Sprout.update(Base.DELETE, this, connection);
				connection.commit();
				success = true;
			}
		}
		catch(SQLException e) {
			connection.rollback();
			throw e;
		}
		finally {
			connection.close();
		}

		return success;
	}

	/**
	 * Inserts or updates the node, it's meta-data and children nodes recursively.
	 * @throws SQLException
	 */
	public void update() throws SQLException {
		Connection connection = Sprout.connection(true);

		try {
			update(connection);
			connection.commit();
		}
		catch(SQLException e) {
			connection.rollback();
			throw e;
		}
		finally {
			connection.close();
		}
	}

	void update(Node node) throws SQLException {
		Connection connection = Sprout.connection(true);

		try {
			link(Base.INSERT, node, connection);
			connection.commit();
		}
		catch(SQLException e) {
			connection.rollback();
			throw e;
		}
		finally {
			connection.close();
		}
	}

	void update(Connection connection) throws SQLException {
		byte action = Base.UPDATE;

		if(id == 0) {
			action = Base.INSERT;
		}

		if(action == Base.INSERT) {
			Sprout.update(action, this, connection);
		}

		Iterator it = meta.iterator();

		while(it.hasNext()) {
			Data data = (Data) it.next();

			if(data.getId() == 0 || (action == Base.UPDATE && !data.done())) {
				Sprout.update(action, data, connection);
				data.done(true);
			}

			if(action == Base.INSERT) {
				meta(action, data, connection);
			}
		}

		it = link.iterator();

		while(it.hasNext()) {
			Node node = (Node) it.next();

			if(action == Base.INSERT) {
				link(action, node, connection);
			}
		}
	}

	void meta(byte action, Data data, Connection connection) throws SQLException {
		if(data.getId() == 0 && !data.done()) {
			Sprout.update(action, data, connection);
		}

		meta.setNode(this);
		meta.setData(data);
		meta.setType(data.getType());

		Sprout.update(action, meta, connection);

		data.done(true);
	}

	void link(byte action, Node node, Connection connection) throws SQLException {
		if(node.getId() == 0) {
			node.update(connection);
		}

		link.setType(getType() | node.getType());
		link.setParent(this);
		link.setChild(node);

		Sprout.update(action, link, connection);
	}

	/**
	 * Find node.
	 * @param id
	 * @return
	 * @throws SQLException
	 */
	public boolean query(long id) throws SQLException {
		NodeBean node = new NodeBean();
		node.setId(id);

		if(Sprout.update(Base.SELECT, node)) {
			copy(node);
			return true;
		}

		return false;
	}

	/**
	 * Is the node parent of child.
	 * @param child
	 * @return
	 * @throws SQLException
	 */
	public boolean parent(Node child) throws SQLException {
		LinkBean link = query(this, child);

		if(link.size() == 1) {
			return true;
		}

		return false;
	}

	/**
	 * Is the node child of parent.
	 * @param parent
	 * @return
	 * @throws SQLException
	 */
	public boolean child(Node parent) throws SQLException {
		LinkBean link = query(parent, this);

		if(link.size() == 1) {
			return true;
		}

		return false;
	}

	/**
	 * Find data/node meta relation where type = value.
	 * You can only query for unique results.
	 * @param type
	 * @param value
	 * @return
	 * @throws SQLException
	 */
	public boolean query(short type, Object value) throws SQLException {
		Data data = new Data();
		data.setType(type);
		data.setValue(value.toString()); // TODO: TEXT or BLOB
		return query(data);
	}

	/**
	 * Find data/node meta relation where type = value.
	 * You can only query for unique results.
	 * @param data
	 * @return
	 * @throws SQLException
	 */
	public boolean query(Data data) throws SQLException {
		if(Sprout.update(Base.SELECT, data)) {
			MetaBean meta = new MetaBean();
			meta.setNode(-1);
			meta.setData(data);
			meta.setType(data.getType());
			meta.setLimit(1);

			Sprout.update(Base.SELECT, meta);

			if(meta.size() == 1) {
				copy((NodeBean) meta.getFirst());
				return true;
			}
		}

		return false;
	}

	protected LinkBean query(Node parent, Node child) throws SQLException {
		return query(parent, child, 1);
	}

	protected LinkBean query(Node parent, Node child, int limit) throws SQLException {
		LinkBean link = new LinkBean();
		link.setType(parent.getType() | child.getType());
		link.setParent(parent);
		link.setChild(child);
		link.setLimit(limit);

		Sprout.update(Base.SELECT, link);

		return link;
	}

	/**
	 * Fills the node with meta-data and children nodes.
	 * Call {@link #query(short, Object)}, {@link #query(Data)} or {@link #query(Node)} first.
	 * Only fetches data from the database the first time.
	 * @param depth
	 * @param start
	 * @param limit
	 * @return
	 * @throws SQLException
	 */
	public boolean fill(int depth, int start, int limit) throws SQLException {
		boolean link = link(start, limit);
		boolean meta = meta();

		if(depth > 0) {
			Iterator it = this.link.iterator();

			while(it.hasNext()) {
				Node node = (Node) it.next();

				// TODO: Cache

				link = node.fill(--depth, start, limit);
			}
		}

		return link && meta;
	}

	/**
	 * Fills the node with children nodes.
	 * Call {@link #query(short, Object)}, {@link #query(Data)} or {@link #query(Node)} first.
	 * Only fetches data from the database the first time.
	 * @param start
	 * @param limit
	 * @return
	 * @throws SQLException
	 */
	public boolean link(int start, int limit) throws SQLException {
		return link(ALL, start, limit);
	}

	protected boolean link(int type, int start, int limit) throws SQLException {
		if(id > 0 && link.size() == 0) {
			link.setParent(this);
			link.setType(type);
			link.setStart(start);
			link.setLimit(limit);
			link.setChild(-1);

			if(Sprout.update(Base.SELECT, link)) {
				for(int i = 0; i < link.size(); i++) {
					NodeBean bean = (NodeBean) link.get(i);
					Node node = new Node();
					node.copy(bean);
					node.done(true);
					link.set(i, node);
				}

				return true;
			}
		}

		return false;
	}

	/**
	 * Fills the node with meta-data.
	 * Call {@link #query(short, Object)}, {@link #query(Data)} or {@link #query(Node)} first.
	 * Only fetches data from the database the first time.
	 * @return
	 * @throws SQLException
	 */
	public boolean meta() throws SQLException {
		if(id > 0 && meta.size() == 0) {
			meta.setNode(this);
			meta.setData(-1);

			if(Sprout.update(Base.SELECT, meta)) {
				for(int i = 0; i < meta.size(); i++) {
					DataBean bean = (DataBean) meta.get(i);
					Data data = new Data();
					data.copy(bean);
					data.done(true);
					meta.set(i, data);
				}

				return true;
			}
		}

		return false;
	}

	/**
	 * Get meta-data safely.
	 * Call {@link #fill(int, int, int)} or {@link #meta()} first.
	 * @param type
	 * @return
	 */
	public String safe(short type) {
		Data safe = meta(type);

		if(safe != null) {
			return safe.getValue();
		}

		return "";
	}

	/**
	 * Get meta-data.
	 * Call {@link #fill(int, int, int)} or {@link #meta()} first.
	 * @param type
	 * @return
	 */
	public Data meta(short type) {
		Iterator it = meta.iterator();

		while(it.hasNext()) {
			Data data = (Data) it.next();

			// TODO: look for date if version

			if(data.getType() == type) {
				return data;
			}
		}

		return null;
	}

	/**
	 * Get child nodes of a certain type.
	 * Call {@link #fill(int, int, int)} or {@link #link(int, int)} first.
	 * @param type
	 * @return
	 */
	public LinkedList child(int type) throws SQLException {
		if(type == ALL) {
			return link;
		}

		Iterator it = link.iterator();
		LinkedList result = new LinkedList();

		while(it.hasNext()) {
			Node node = (Node) it.next();

			// TODO: look for date if version

			if(node.getType() == type) {
				node.meta();
				result.add(node);
			}
		}

		return result;
	}

	/**
	 * Return the first child that contains the meta-data.
	 * Call {@link #fill(int, int, int)} or {@link #link(int, int)} first.
	 * @param link The node type.
	 * @param meta The data type.
	 * @param value
	 * @return
	 * @throws SQLException
	 */
	public Node child(int link, short meta, String value) throws SQLException {
		Iterator it = this.link.iterator();

		while(it.hasNext()) {
			Node node = (Node) it.next();

			if(node.getType() == link) {
				node.meta();

				if(node.meta(meta).getValue().equals(value)) {
					return node;
				}
			}
		}

		return null;
	}

	/**
	 * Return the first parent of a type that contains the meta-data.
	 * @param type
	 * @param data
	 * @param limit How many parents to select
	 * @return
	 * @throws SQLException
	 */
	public Node parent(int type, Data data, int limit) throws SQLException {
		LinkBean link = query(new Node(type, -1), this, limit);
		Iterator it = link.iterator();

		while(it.hasNext()) {
			Node node = new Node();
			node.copy((NodeBean) it.next());
			node.meta();

			if(node.meta(data.getType()).getValue().equals(data.getValue())) {
				return node;
			}
		}

		return null;
	}

	/**
	 * Return the first parent of a type.
	 * @param type
	 * @return
	 * @throws SQLException
	 */
	public Node parent(int type) throws SQLException {
		LinkBean link = query(new Node(type, -1), this, 1);
		Iterator it = link.iterator();

		while(it.hasNext()) {
			Node node = new Node();
			node.copy((NodeBean) it.next());
			return node;
		}

		return null;
	}

	/**
	 * Get child node.
	 * Call {@link #fill(int, int, int)} or {@link #link(int, int)} first.
	 * @param id
	 * @return
	 */
	public Node child(long id) throws SQLException {
		Iterator it = link.iterator();

		while(it.hasNext()) {
			Node node = (Node) it.next();

			if(node.getId() == id) {
				return node;
			}
		}

		return null;
	}
	/*
	public String path() {
		return "/upload/" + date() + "/";
	}
	 */
	public String path() {
		String token = String.valueOf(id);
		StringBuffer path = new StringBuffer();

		for(int i = 0; i < token.length(); i++) {
			path.append("/");
			path.append(token.charAt(i));
		}

		return path.toString();
	}

	public String encoded() throws UnsupportedEncodingException {
		return URLEncoder.encode(path(), "UTF-8");
	}

	public String date() {
		return date.format(new Date(getDate()));
	}

	public String time() {
		return time.format(new Date(getDate()));
	}

	protected static boolean cache(int link, Data name) {
		HashMap hash = (HashMap) cache.get(new Integer(link));

		if(hash == null) {
			hash = new HashMap();
			cache.put(new Integer(link), hash);
		}

		try {
			Node node = new Node(link);

			if(!node.query(name)) {
				node = new Node(link);
				node.add(name);
				node.update();
			}

			hash.put(name.getValue(), node);
			return true;
		}
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	protected static Node cache(int link, String name) {
		HashMap hash = (HashMap) cache.get(new Integer(link));

		if(hash == null) {
			System.out.println("Node cache for '" + name + "' is empty. (" + link + ")");
		}
		else {
			return (Node) hash.get(name);
		}

		return null;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		print(buffer, 0);
		return buffer.toString();
	}

	public String toXML(int padding) {
		StringBuffer buffer = new StringBuffer();
		print(buffer, padding);
		return buffer.toString();
	}

	protected void padding(StringBuffer buffer, int level) {
		for(int i = 0; i < level; i++) {
			buffer.append("  ");
		}
	}

	protected void print(StringBuffer buffer, int level) {
		padding(buffer, level);
		buffer.append("<node id=\"" + getId() + "\" type=\"" + getType() + "\">\n");

		Iterator it = meta.iterator();

		while(it.hasNext()) {
			Data data = (Data) it.next();

			padding(buffer, level + 1);
			if(data.getValue().length() > 100) {
				buffer.append("<meta id=\"" + data.getId() + "\" type=\"" + data.getType() + "\">\n");
				padding(buffer, level + 1);
				buffer.append(data.getValue() + "\n");
				padding(buffer, level + 1);
				buffer.append("</meta>\n");
			}
			else {
				buffer.append("<meta id=\"" + data.getId() + "\" type=\"" + data.getType() + "\" value=\"" + data.getValue() + "\"/>\n");
			}
		}

		it = link.iterator();

		while(it.hasNext()) {
			Node node = (Node) it.next();
			node.print(buffer, level + 1);
		}

		padding(buffer, level);
		buffer.append("</node>\n");
	}

	/*
	 * Remove count from poll
	 */
	public boolean remove(short type) throws Exception {
		PollBean poll = new PollBean();
		poll.setType(type);
		poll.setNode(this);

		if(Sprout.update(Base.SELECT, poll)) {
			if(poll.size() == 1) {
				poll = (PollBean) poll.getFirst();
				Sprout.update(Base.DELETE, poll);
				return true;
			}
		}
		else {
			return true;
		}

		return false;
	}

	public boolean poll(short type) throws Exception {
		return poll(type, 1);
	}

	/*
	 * Add count to poll
	 */
	public boolean poll(short type, int increment) throws Exception {
		PollBean poll = new PollBean();
		poll.setNode(this);
		poll.setType(type);

		if(Sprout.update(Base.SELECT, poll)) {
			if(poll.size() == 1) {
				poll = (PollBean) poll.getFirst();
			}
			else {
				throw new Exception("Multiple " + type + " found.");
			}

			poll.setValue(poll.getValue() + increment);
			Sprout.update(Base.UPDATE, poll);
			return false;
		}
		else {
			poll.setValue(poll.getValue() + increment);
			Sprout.update(Base.INSERT, poll);
			return true;
		}
	}

	public double count(short type) throws Exception {
		PollBean poll = new PollBean();
		poll.setNode(this);
		poll.setType(type);

		if(Sprout.update(Base.SELECT, poll)) {
			if(poll.size() == 1) {
				poll = (PollBean) poll.getFirst();
			}
			else {
				throw new Exception("Multiple " + type + " found.");
			}

			return poll.getValue();
		}

		return -1;
	}
}
