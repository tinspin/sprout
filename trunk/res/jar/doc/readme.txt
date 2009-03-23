
                 Sprout

              ------o------

Sprout is a simple base from which you can build 
any CMS in Java.

You will need Mysql 4.0 or later and Java 1.4 or 
later added to the path to run Sprout.

You need Ant added to the path to build Sprout.

Run the server with run.bat or run.sh depending 
on your OS and build sprout with ant:

  > run.bat | run.sh &
  
  Add these to the run script after java if: 
  -Ddbuser=... and
  -Ddbpass=... if your database user is other than "root"/"" and 
  -Ddburl=... if you choose another database name.
  
  > ant

Then simply create a database like this:

  > CREATE DATABASE sprout DEFAULT CHARACTER SET utf8 COLLATE utf8_bin;

And pipe the create.sql to it like this:

  > mysql -uXXXX -p sprout < create.sql

Finally browse to: http://localhost:9000

To localise your site; edit i18n.txt, mail.txt and uncomment the 
Sprout.i18n() method.

To view the database structure, double click lib/logic.jar and
open res/sprout.xml, you should not need to modify the database
but you can easily do so in logic and save the sprout.xml. The 
changes will be executed when you run the ant script.

The node graph for the content demo code is very simple:

  User    --- (n) Groups

  Article -+- (1) User
           |
           +- (n) Files
           |
           +- (n) Comments

  Comment --- (1) User

Now just edit the Type.java types to reflect the model your project 
requires and write the HTML to manipulate this model.

Happy Hacking!

Version:

  0.1 Alpha
      - Users
      - Articles
      - Comments
      - File Upload
      - Flash Video & Audio
      - Pagination
      - Translation

Todo:

  - Integrate SortPoll with Node, test with most read articles.
  - Smaller title font in content.
  - Encrypt email and password from a key that you input upon 
    starting the server, backwards compability.
  - Track- / Ping- backs.
  - Hide/show articles, with selective select.
  - Cascade delete for article child nodes (except user!).
  - Delete comment for admin.
  - Edit and hide comment for session.
  - Delete files
  - Cache timeout
  - Add editor privilege, disable it by default.
  - Generic admin.
  - Categories, test with swedish/english.
  - Localized dates.
