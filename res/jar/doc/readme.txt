
                 Sprout

              ------o------

Sprout is a simple blogger from which you can 
build any site.

You will need Mysql 4.0, Java 1.4 or later and 
Ant added to the path to build and run Sprout.

Run the server with run.bat or run.sh depending 
on your OS and build sprout with ant:

  > run.bat | run.sh &
  
  # Default mysql user is root with empty string as 
  # password if you want to use other credentials just 
  # add -Ddbuser=XXX -Ddbpass=YYY to the run.bat or run.sh, 
  # where XXX should be replaced with your user and YYY with 
  # your password.
  
  > ant

Then simply create a database like this:

  > CREATE DATABASE sprout DEFAULT CHARACTER SET utf8 COLLATE utf8_bin;

And pipe the create.sql to it like this:

  > mysql -uXXX -p sprout < create.sql

Finally browse to: http://localhost:9000

  # IMPORTANT: Now that you got sprout up and running, 
  # change the pass from 'secret' to something secure 
  # in the run.bat, run.sh and build.xml files.

  # You can change the name of the whole project in the 
  # build.xml file if you will run multiple sprout, the 
  # name is used for the database name per default so the 
  # tutorial above has to be modified to suit the new name.

  # Don't forget to add -Ddburl=jdbc:mysql://localhost/ZZZ 
  # to the run.bat and run.sh if you choose another database 
  # name, where ZZZ should be replaced with your database name.

  # You can change the port in the run.bat, run.sh and 
  # build.xml files if you will run multiple instances 
  # of sprout on the same machine. Then it's recommended 
  # to virtual host proxy the sites with an apache up front.

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
      - Search

  0.2 Beta
      - Columization
      - Node poll
      - Pingback

Todo:

  - Session lock on ARTICLE_READ.
  - Delete files.
  - Make a new video player to match the audio player (that does not autostart).
  - Cascade delete for article child nodes (except user!), link bug.
  - Delete comment for admin.
  - Edit and/or hide comment for session.
  - Cache timeout.
  - Add editor privilege, disable it by default.
  - Generic admin.
  - Categories, test with swedish/english.
  - Localized dates.
  - Hide/show articles, with selective select, apply to comments.
