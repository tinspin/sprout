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
 * TODO: require {@link update()} call for post insert adds.
 * @author Marc
 */
public class Node extends NodeBean implements Type {
	public static HashMap cache = new HashMap();
	private static Data none = new Data();
	
	static Format time = new SimpleDateFormat("yy/MM/dd HH:mm:ss");
	static Format date = new SimpleDateFormat("yy/MM/dd");

	private LinkBean link;
	private MetaBean meta;

	/**
	 * The node type should be a bit identifiable integer
	 * this limits the number of node types to 32.
	 * @param type
	 */
	public Node(int type) {
		this();
		setType(type);
	}

	Node() {
		link = new LinkBean();
		meta = new MetaBean();
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

		if(id > 0 && (node.getId() == 0 || get(node.getId()) == null)) {
			update(node);
		}

		link.add(0, node);
	}

	/**
	 * Remove the child node.
	 * @param node
	 * @return
	 * @throws SQLException
	 */
	public boolean remove(Node node) throws SQLException {
		if(link.contains(node)) {
			link.setParent(this);
			link.setChild(node);
			link.setType(getType() | node.getType());

			Sprout.update(Base.DELETE, link);
			
			return true;
		}

		return false;
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

	public void add(Data data) throws SQLException {
		if(data == null) {
			throw new NullPointerException("Can't add data.");
		}

		Data old = get(data.getType());

		if(old != null) {
			meta.remove(old);

			Data cache = Data.cache(type, data.getValue());

			if(cache != null) {
				remove(old);
				meta(Base.INSERT, data, Sprout.connection(false));
			}
			else {
				data.setId(old.getId());
			}
		}

		if(id > 0 && data.getId() == 0) {
			meta(Base.INSERT, data, Sprout.connection(false));
		}

		meta.add(data);
	}

	/**
	 * Remove meta-data.
	 * @param data
	 * @return
	 * @throws SQLException
	 */
	public boolean remove(Data data) throws SQLException {
		if(meta.contains(data)) {
			meta.setNode(this);
			meta.setData(data);
			meta.setType(data.getType());

			Sprout.update(Base.DELETE, meta);
			
			return true;
		}

		return false;
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

		Sprout.update(action, this, connection);
		Iterator it = meta.iterator();

		while(it.hasNext()) {
			Data data = (Data) it.next();

			if(data.getId() == 0 || action == Base.UPDATE) {
				Sprout.update(action, data, connection);
			}

			if(action == Base.INSERT) {
				meta(action, data, connection);
			}
		}

		it = link.iterator();

		while(it.hasNext()) {
			Node node = (Node) it.next();
			link(action, node, connection);
		}
	}

	void meta(byte action, Data data, Connection connection) throws SQLException {
		if(data.getId() == 0) {
			Sprout.update(action, data, connection);
		}

		meta.setNode(this);
		meta.setData(data);
		meta.setType(data.getType());

		Sprout.update(action, meta, connection);
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

	/**
	 * Find parent node. You can only query for unique results.
	 * @param node
	 * @return
	 * @throws SQLException
	 */
	public boolean query(Node node) throws SQLException {
		LinkBean link = new LinkBean();
		link.setType(getType() | node.getType());
		link.setParent(-1);
		link.setChild(node);
		link.setLimit(1);

		Sprout.update(Base.SELECT, link);

		if(link.size() == 1) {
			copy((NodeBean) link.getFirst());
			return true;
		}

		return false;
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

	boolean link(int type, int start, int limit) throws SQLException {
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
					meta.set(i, data);
				}

				return true;
			}
		}

		return false;
	}

	/**
	 * Get meta-data.
	 * Call {@link #fill(int, int, int)} or {@link #meta()} first.
	 * @param type
	 * @return
	 */
	public Data get(short type) {
		Iterator it = meta.iterator();

		while(it.hasNext()) {
			Data data = (Data) it.next();

			// TODO: look for date if version

			if(data.getType() == type) {
				return data;
			}
		}

		return none;
	}

	/**
	 * Get child nodes of a certain type.
	 * Call {@link #fill(int, int, int)} or {@link #link(int, int)} first.
	 * @param type
	 * @return
	 */
	public LinkedList get(int type) throws SQLException {
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
	public Node get(int link, short meta, String value) throws SQLException {
		Iterator it = this.link.iterator();

		while(it.hasNext()) {
			Node node = (Node) it.next();

			if(node.getType() == link) {
				node.meta();

				if(node.get(meta).getValue().equals(value)) {
					return node;
				}
			}
		}

		return null;
	}

	/**
	 * Get child node.
	 * Call {@link #fill(int, int, int)} or {@link #link(int, int)} first.
	 * @param id
	 * @return
	 */
	public Node get(long id) throws SQLException {
		Iterator it = link.iterator();

		while(it.hasNext()) {
			Node node = (Node) it.next();

			if(node.getId() == id) {
				return node;
			}
		}

		return null;
	}

	public String path() {
		return "/upload/" + date() + "/";
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
			System.out.println("Node cache is empty for " + name + ". (" + link + ")");
		}

		return (Node) hash.get(name);
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		print(buffer, 0);
		return buffer.toString();
	}

	void padding(StringBuffer buffer, int level) {
		for(int i = 0; i < level; i++) {
			buffer.append("    ");
		}
	}

	void print(StringBuffer buffer, int level) {
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
}
