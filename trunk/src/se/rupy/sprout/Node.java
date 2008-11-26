package se.rupy.sprout;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.*;
import java.util.*;

import se.rupy.memory.*;
import se.rupy.pool.*;

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
	 * Add meta-data. Does not insert to the database, call {@link #update()} to persist. 
	 * Additions can only be made prior to the {@link #update()} call.
	 * @param type
	 * @param value
	 * @throws SQLException
	 */
	public void add(short type, String value) throws SQLException {
		DataBean data = new DataBean();
		data.setType(type);
		data.setValue(value);
		add(data);
	}

	void add(DataBean data) throws SQLException {
		DataBean old = get(data.getType());
		
		if(old != null) {
			meta.remove(old);
			data.setId(old.getId());
		}
		else if(id > 0) {
			meta.setNode(this);
			meta.setData(data);
			meta.setType(data.getType());
			Sprout.update(Base.INSERT, meta);
		}
		
		meta.add(data);
	}

	/**
	 * Inserts or updates the node, it's meta-data and children nodes.
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
			update(Base.INSERT, node, connection);
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

	/**
	 * Delete the child node.
	 * @param node
	 * @throws SQLException
	 */
	public void delete(Node node) throws SQLException {
		Connection connection = Sprout.connection(true);

		try {
			delete(node, connection);
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
			DataBean data = (DataBean) it.next();
			Sprout.update(action, data, connection);

			if(action == Base.INSERT) {
				meta.setNode(this);
				meta.setData(data);
				meta.setType(data.getType());
				Sprout.update(action, meta, connection);
			}
		}

		it = link.iterator();

		while(it.hasNext()) {
			Node node = (Node) it.next();
			update(action, node, connection);
		}
	}

	void update(byte action, Node node, Connection connection) throws SQLException {
		if(node.getId() == 0) {
			node.update(connection);
		}

		link.setParent(this);
		link.setChild(node);
		link.setType(getType() | node.getType());
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
		DataBean data = new DataBean();
		data.setType(type);
		data.setValue(value.toString()); // TODO: TEXT or BLOB
		return query(data);
	}

	boolean query(DataBean data) throws SQLException {
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
	 * Find children nodes. Call {@link #query(short, Object)} or {@link #query(DataBean)} first.
	 * Only fetches data from the database the first time.
	 * @param type
	 * @return
	 * @throws SQLException
	 */
	public boolean link(int type) throws SQLException {
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
	 * Load meta-data. Call {@link #query(short, Object)} or {@link #query(DataBean)} first. 
	 * Only fetches data from the database the first time.
	 * @throws SQLException
	 */
	public void meta() throws SQLException {
		if(id > 0 && meta.size() == 0) {
			meta.setNode(this);
			meta.setData(-1);
			Sprout.update(Base.SELECT, meta);
		}
	}

	/**
	 * Get meta-data. Call {@link meta()} first.
	 * @param type
	 * @return
	 */
	public DataBean get(short type) {
		Iterator it = meta.iterator();

		while(it.hasNext()) {
			DataBean data = (DataBean) it.next();

			// TODO: look for date if version

			if(data.getType() == type) {
				return data;
			}
		}

		return null;
	}

	/**
	 * Get child nodes. Call {@link link(int)} first.
	 * @param type
	 * @param fill
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
	 * Checks if any of the children contains the meta-data.
	 * @param type
	 * @param meta
	 * @param value
	 * @return
	 * @throws SQLException
	 */
	public Node contains(int type, short meta, String value) throws SQLException {
		Iterator it = link.iterator();

		while(it.hasNext()) {
			Node node = (Node) it.next();
			
			if(node.getType() == type) {
				node.meta();
				
				if(node.get(meta).getValue().equals(value)) {
					return node;
				}
			}
		}

		return null;
	}
	
	/**
	 * Get child node. Call {@link link(int)} first.
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
	
	void delete(Node node, Connection connection) throws SQLException {
		link.setParent(this);
		link.setChild(node);
		link.setType(getType() | node.getType());
		Sprout.update(Base.DELETE, link, connection);
		
		// TODO: Verify link for other references.
		Sprout.update(Base.DELETE, node, connection);
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
}