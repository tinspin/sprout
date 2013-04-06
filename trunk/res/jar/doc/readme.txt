
                 Sprout

              ------o------

Sprout is a simple blogger from which you can 
build any site. Now works with PostgreSQL 
(see 2.A.b) and Oracle (see 2.A.c.)!

----------------------------------------------

1. Quick Tutorial:

   You will need Mysql 4.0, Java 1.5 or later and 
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
  
   Build sprout. You need to run ant twice when building from 
   scratch, since it generates dependencies that won't be in the 
   classpath during the first build.
  
     > ant
     > ant

   Then simply create a database like this:

     > mysql -uXXX -p
     > CREATE DATABASE sprout;

   And pipe the create.sql to it like this:

     > mysql -uXXX -p sprout < create.sql

   Finally browse to: http://localhost:9000

----------------------------------------------

2. Setting up a live sprout instance:

   ##IMPORTANT##

   Since NIO has been very unstable from 
   1.6.0_00 until 1.6.0_18 with Selector bugs 
   ranging from 100% CPU to thread deadlock; 
   you should use 1.5 or 1.6.0_18 and later 
   on your live server.

   A. Databases

      a. MySQL

         Make sure you have MySQL running with innodb_file_per_table.
         And pipe the alter.sql to it.
   
         > mysql -uXXX -p sprout < alter.sql

      b. PostgreSQL

         To use PostgreSQL edit build.xml:
   
         <property name="db" value="postgresql"/>
   
         Then add the following parameters to the run script after 
         you edit them (XXX, YYY and ZZZ):

         IMPORTANT: ZZZ should be sprout unless you choosed to do 2.B.b.

         -Ddburl=jdbc:postgresql:ZZZ
         -Ddbdriver=org.postgresql.Driver
         -Ddbuser=XXX
         -Ddbpass=YYY
   
         Finally create the database like this:

         - Open SQL Shell (psql).
         - Create the database: # create database ZZZ;
         - Switch database: # \c ZZZ
         - Pipe create script: # \i 'PATH/create.sql' where you 
           replace PATH with where you extracted sprout. On 
           windows you need to replace all '\' in the path with '/'
         - Pipe alter.sql script after editing it.

      c. Oracle

         To use Oracle edit build.xml:
   
         <property name="db" value="oracle"/>
   
         Then add the following parameters to the run script after 
         you edit them (XXX=ZZZ, YYY):

         IMPORTANT: The database name is linked to the user so if you want 
         to 2.B.b. then you simply create a new user with that name.

         -Ddburl=jdbc:oracle:thin:@localhost:1521:xe
         -Ddbdriver=oracle.jdbc.OracleDriver
         -Ddbuser=XXX (the same as the database name, or ZZZ in other configs)
         -Ddbpass=YYY

         Finally create the database like this:

         - Install OracleXE, remember the SYS password.
         - Run 'Go To Database Home Page' in:
           Start Menu -> Programs -> Oracle Database 10g Express Edition
         - Login with user-name SYS and the password you entered during 
           the installation.
         - Add a new user; with the same name as the sprout instance ZZZ=XXX 
           and YYY as password, with all privileges then logout.
           (Home > Administration > Database Users > Create User)
         - Login to Oracle again, this time as the ZZZ=XXX user; upload and 
           run the generated create.sql script generated earlier 
           (Home > SQL > SQL Scripts > Upload).
         - Run the alter.sql script after editing it.

   B. Sprout Configurations

      a. Hot-deployment password

         Now that you got sprout up and running, change the 
         pass from 'secret' to something secure in the run.bat, 
         run.sh and build.xml files if you want to be able to 
         hotdeploy from a remote host.

      b. Change project name from sprout

         You can change the name of the whole project in the 
         build.xml file if you will run multiple sprout, the 
         name is used for the database name per default so the 
         tutorial above has to be modified to suit the new name.

         Don't forget to add -Ddburl=jdbc:mysql://localhost/ZZZ 
         to the run.bat and run.sh and change the name of the 
         res/sprout.xml file to res/ZZZ.xml if you choose another 
         database name, where ZZZ should be replaced with your new 
         name.

         For Oracle see 2.A.c.

      c. Host multiple sprout on different ports

         You can change the port in the run.bat, run.sh and 
         build.xml files if you will run multiple instances 
         of sprout on the same machine. Then it's recommended 
         to virtual host proxy the sites with an apache up front.

      d. Server properties

         You also need to edit or add the -Dhost=sprout.rupy.se, 
         -Dmail=mail1.comhem.se and -Daddress=info@yourcompany.com 
         properties in the run.sh (or add and alter them to run.bat 
         if you run your live server on windows) file to match your 
         site.
         
   C. host.rupy.se
   
      If you are using our integrated hosting service you need to edit
      Sprout.SQL following the instructions in the class file.
      
      You also need to deploy the jar as your.domain.name.jar which 
      means changing the name property from sprout to your.domain.name 
      in build.xml and renaming the res/sprout.xml file to 
      res/your.domain.name.xml

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

4. Version:

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
      
   0.2.1 Database Stable
  
      IMPORTANT: NOT backwards compatible!
  
      - Revised how uploaded files are stored. Now it makes sense and is scalable!
      - Changed data value from TEXT to BLOB in the database, so we can store binaries.
      - Optimised node UPDATE to only update updated meta-data fields.
  
      - Password Reminder
      - User Profile (Birthday, Country, First Name, Last Name and 
        Gender with public options)
      - Drag'n Drop articles on frontpage. (just for fun)
      - AJAX Login and Post. (just for fun)
      - GeoIP and country flags.
      - Comet Chat. Download dependency from rupy project and deploy on server!
      - Edit profile.
      - Profile image.
      
   0.2.2 PostgreSQL and Oracle
  
      - Profile page
      - Fixed resource upload order, first you insert an article then you can add 
        resources to it!
        
   0.2.3 Rupy "Stable" Release
   
      - Now Rupy is Industrial Strength.
      
   0.2.4 Rupy Fix Release
   
      - Fixed socket file-descriptor leak in Rupy.
      
   0.2.5 Rupy Fix Release
   
      - Fixed file file-descriptor leak in Rupy.
      - Java 1.7 removes com.sun.image.codec.jpeg
      
----------------------------------------------

5. Todo:

   - Generic Node-Graph to SQL transform for complex queries.

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
