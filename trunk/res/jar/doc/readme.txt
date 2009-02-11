
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

To view the database structure, double click lib/logic.jar and
open res/sprout.xml, you should not need to modify the database
but you can easily do so in logic and save the sprout.xml. The 
changes will be executed when you run the ant script.

Now just edit the Type.java types to reflect the model your project 
requires and write the HTML to manipulate this model.

Happy Hacking!

Version:

  0.1 Alpha
      - Blog/Community/CMS Lite. 

Todo:

  - Delete images
  - Pagination
  - Cache Timeout