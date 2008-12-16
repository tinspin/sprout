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
		link.add(0, node);

		if(id > 0 && node.getId() == 0) {
			update(node);
		}
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

	void add(Data data) throws SQLException {
		Data old = get(data.getType());
		
		if(old != null) {
			meta.remove(old);
			data.setId(old.getId());
		}
		
		if(id > 0 && data.getId() == 0) {
			meta(Base.INSERT, data, Sprout.connection(false));
		}
		
		meta.add(data);
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
	 * Find data/node meta relation where type = value and populate this node.
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

	boolean query(Data data) throws SQLException {
		if(Sprout.update(Base.SELECT, data)) {
			MetaBean meta = new MetaBean();
			meta.setNode(-1);
			meta.setData(data);
			meta.setType(data.getType());
			Sprout.update(Base.SELECT, meta);

			if(meta.size() == 1) {
				NodeBean node = (NodeBean) meta.getFirst();
				copy(node);
				return true;
			}
		}

		return false;
	}
	
	/**
	 * Fills the node with meta-data and children nodes.
	 * Call {@link #query(short, Object)} or {@link #query(Data)} first.
	 * Only fetches data from the database the first time.
	 * @param recursively Fill all children nodes recursively.
	 * @return
	 * @throws SQLException
	 */
	public boolean fill(boolean recursively) throws SQLException {
		boolean link = link();
		boolean meta = meta();
		
		if(recursively) {
			Iterator it = this.link.iterator();

			while(it.hasNext()) {
				Node node = (Node) it.next();
				
				// TODO: Cache
				
				link = node.fill(true);
			}
		}
		
		return link && meta;
	}
	
	/**
	 * Fills the node with children nodes.
	 * Call {@link #query(short, Object)} or {@link #query(Data)} first.
	 * Only fetches data from the database the first time.
	 * @return
	 * @throws SQLException
	 */
	public boolean link() throws SQLException {
		return link(ALL);
	}
	
	boolean link(int type) throws SQLException {
		if(id > 0 && link.size() == 0) {
			link.setParent(this);
			link.setType(type);
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
	 * Call {@link #query(short, Object)} or {@link #query(Data)} first. 
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
	 * Call {@link #fill(boolean)} or {@link #meta()} first.
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

		return null;
	}

	/**
	 * Get child nodes of a certain type.
	 * Call {@link #fill(boolean)} or {@link #link()} first.
	 * @param type
	 * @return
	 */
	public LinkedList get(int type) throws SQLException {
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
	 * Call {@link #fill(boolean)} or {@link #link()} first.
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
	 * Call {@link #fill(boolean)} or {@link #link()} first.
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
	
	static HashMap cache = new HashMap();
	
	static boolean cache(int link, short meta, String[] name) {
		HashMap hash = new HashMap();
		
		try {
			for(int i = 0; i < name.length; i++) {
				Node node = new Node(link);
				
				if(node.query(meta, name[i])) {
					node.fill(false);
					hash.put(name[i], node);
				}
				else {
					return false;
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		cache.put(new Integer(link), hash);
		
		return true;
	}
	
	static Node cache(int link, short meta, String name) {
		HashMap node = (HashMap) cache.get(new Integer(link));
		return (Node) node.get(name);
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
