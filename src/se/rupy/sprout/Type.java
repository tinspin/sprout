package se.rupy.sprout;

public interface Type {
	/*
	 * Node
	 */
	public final static int ALL			= 0;
	public final static int USER		= 1 << 0; // 1
	public final static int ARTICLE		= 1 << 1; // 2
	public final static int COMMENT		= 1 << 2; // 4
	public final static int CATEGORY	= 1 << 3; // 8
	public final static int GROUP		= 1 << 4; // 16
	public final static int FILE		= 1 << 5; // 32
	public final static int PING		= 1 << 6; // 64
	
	/*
	 * Data
	 */
	public final static short USER_NAME			= 100;
	public final static short USER_PASS			= 101;
	public final static short USER_MAIL			= 102;
	public final static short USER_KEY			= 103;
	public final static short USER_IP			= 104;
	public final static short USER_STATE		= 105;
	public final static short USER_FIRST_NAME	= 106;
	public final static short USER_LAST_NAME	= 107;
	public final static short USER_BUG_ID		= 108;
	public final static short USER_BUG_NAME		= 109;
	public final static short USER_COUNTRY		= 110;
	public final static short USER_BIRTHDAY		= 111;
	public final static short USER_GENDER		= 112;
	public final static short USER_SHOW			= 113;
	
	public final static short ARTICLE_TITLE		= 200;
	public final static short ARTICLE_BODY		= 201;
	public final static short ARTICLE_READ		= 202;
	public final static short ARTICLE_RATING	= 203;
	public final static short ARTICLE_TYPE		= 204;
	public final static short COMMENT_BODY		= 300;
	public final static short COMMENT_IP		= 301;
	public final static short COMMENT_STATE		= 302;
	public final static short CATEGORY_NAME		= 400;
	public final static short GROUP_NAME		= 500;
	public final static short FILE_NAME			= 600;
	public final static short FILE_TYPE			= 601;
	public final static short PING_TITLE		= 700;
	public final static short PING_URL			= 701;
	public final static short PING_STATE		= 702;
}
