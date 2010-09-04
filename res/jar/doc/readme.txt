
                 Sprout

              ------o------

Sprout is a simple blogger from which you can 
build any site. Now works with Postgres, see 2.d.!

----------------------------------------------

1. Quick Tutorial:

You will need Mysql 4.0, Java 1.4 or later and 
Ant added to the path to build and run Sprout.

Run the server with run.bat or run.sh depending 
on your OS and build sprout with ant:

  > run.bat | run.sh &
  
  # IMPORTANT: If you use unix derivative for development 
  # you should probably remove the -live flag from the .sh 
  # script. The live flag affects e-mail verification upon 
  # registering and hard caching of static HTTP requests.
  
  # Default mysql user is root with empty string as 
  # password if you want to use other credentials just 
  # add -Ddbuser=XXX -Ddbpass=YYY to the run.bat or run.sh, 
  # where XXX should be replaced with your user and YYY with 
  # your password.
  
  > ant

Then simply create a database like this:

  > mysql -uXXX -p
  > CREATE DATABASE sprout;

And pipe the create.sql to it like this:

  > mysql -uXXX -p sprout < create.sql

Finally browse to: http://localhost:9000

----------------------------------------------

2. Setting up a live sprout instance:

a. MySQL

   Make sure you have MySQL running with innodb_file_per_table.
   And pipe the alter.sql to it.
   
   > mysql -uXXX -p sprout < alter.sql

b. Hot-deployment password

   Now that you got sprout up and running, change the 
   pass from 'secret' to something secure in the run.bat, 
   run.sh and build.xml files if you want to be able to 
   hotdeploy from a remote host.

c. Change project name from sprout

   You can change the name of the whole project in the 
   build.xml file if you will run multiple sprout, the 
   name is used for the database name per default so the 
   tutorial above has to be modified to suit the new name.

   Don't forget to add -Ddburl=jdbc:mysql://localhost/ZZZ 
   to the run.bat and run.sh and change the name of the 
   res/sprout.xml file to res/ZZZ.xml if you choose another 
   database name, where ZZZ should be replaced with your new 
   name.

d. Postgres

   To use Postgres instead of MySQL edit build.xml:
   
   <property name="db" value="postgres"/>
   
   Then add the following parameters to the run script after 
   you edit them (XXX, YYY and ZZZ):

   IMPORTANT: ZZZ should be sprout unless you choosed to do 2.c.

   -Ddburl=jdbc:postgresql:ZZZ
   -Ddbdriver=org.postgresql.Driver
   -Ddbuser=XXX
   -Ddbpass=YYY
   
   Finally create the database like this:

   - Open SQL Shell (psql).
   - Create the database: # create database ZZZ;
   - Switch database: # \c ZZZ
   - Pipe create script: # \i 'PATH/create.sql' where you 
     replace PATH with where you extracted this test. On 
     windows you need to replace all '\' in the path with '/'
   - Pipe alter script (alter.sql).

e. Host multiple sprout on different ports

   You can change the port in the run.bat, run.sh and 
   build.xml files if you will run multiple instances 
   of sprout on the same machine. Then it's recommended 
   to virtual host proxy the sites with an apache up front.

f. Server properties

   You also need to edit or add the -Dhost=sprout.rupy.se, 
   -Dmail=mail1.comhem.se and -Daddress=info@yourcompany.com 
   properties in the run.sh (or add and alter them to run.bat 
   if you run your live server on windows) file to match your 
   site.

----------------------------------------------

3. Development tunings:

To localise your site; edit i18n.txt, mail.txt and change the 
translate property in the i18n.txt file to true.

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

----------------------------------------------

5. Version:

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
      
  0.2.1 IMPORTANT: NOT backwards compatible!
  
      - Password Reminder
      - User Profile (Birthday, Country, First Name, Last Name and 
        Gender with public options)
      - Drag'n Drop articles on frontpage. (just for fun)
      - AJAX Login and Post. (just for fun)
      - GeoIP and country flags.
      - Comet Chat. Download dependency from rupy project and deploy on server!
      - Edit profile.
      - Profile image.
      
      - Revised how uploaded files are stored. Now it makes sense and is scalable!
      - Changed data value from TEXT to BLOB in the database, so we can store binaries.
      - Optimised node UPDATE to only update updated meta-data fields.
      
  0.2.2 Postgres Compatible!
  
      - Profile page
      - Fixed resource upload order, first you insert an article then you can add 
        resources to it!
      
----------------------------------------------

5. Todo:

  - Hide/show articles with date.
  - Labels, test with swedish/english.
  - Verify only characters on nickname.

  - Cache timeout.
  - Delete files.

  - Vote!
  - Captcha?
  - Session lock on ARTICLE_READ.
  - Make a new video player to match the audio player (that does not autostart).
  - Add editor privilege, enable by default.
  - Hide comment alternative for session.
  - Localized dates.
