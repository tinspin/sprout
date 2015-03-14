<font color='red'><b><i>NEW</i></b></font> [µSOA](https://code.google.com/p/rupy/wiki/Process) is the better way of server development, and I will port Sprout to [ROOT](https://code.google.com/p/rupy/wiki/Persistence) when I get the time.

Because a big part of what programmers do is to imagine typed data hierarchies that they then store with SQL, wrap in XML or JSON and send around; which in it's turn gets unwrapped and presented in a similar but often separate implementation on the other end. That data is then modified and sent all the way back to be stored again.

With a node-graph data model you only store, send and present nodes. The hierarchy is infront of the typing; just like in a file system.

So all the code to store, present, modify, search, etc. the nodes is the same. And if you improve some part of the code, it can more often than not, be reused on other nodes with a few generalisations.

> _**TYPE** Strongly Typed Data (Type -> Hierarchy):_

| Pro | Con |
|:----|:----|
| You can add more "junior" developers to the project with linearly increased productivity. | You write alot of typed code that can't be reused. |
| You can replace developers "quickly". | API grows linearly with number of objects. |

> _**HIERARCHY** Hierarchical Node Data (Hierarchy -> Type):_

| Pro | Con |
|:----|:----|
| API stays the same (insert, update, delete and select node, see [Node](http://rupy.se/sprout/se/rupy/sprout/Node.html) javadoc for more info). | Demands alot from the programmer. |
| Lots of methods become generic and reuse is inherently encouraged. |  |
| You can easily alter/hotdeploy the **_whole_** system in realtime without interruption. |  |
| The bridge to scalable key/value storage is faster with node-graph since the node-graph **IS** a hierachical key/value store. |  |

The arguments will always be pro _type_ in a big corporation and pro _hierarchy_ in a smaller startup.

The point is: Either your code is modular or your developers are, the choice is easy!

**The only real problem with node graph databases is that you have to describe the hierarchy of the nodes so that other people can work on your data.**

For example for the search below the structure looks like this:

```
article -+- user
```

And there is no way someone can figure it out without having to go through the code that creates this structure or select a user that has written articles and look at the nodetree.


---


This is what the database layout looks like. You can see the create database script [here](http://code.google.com/p/sprout/source/browse/trunk/create.sql). A Node-Graph system is basically a filesystem; as in all filesystems the file types are subordinate to the file hierarchy; A folder contains files that contain data that has a type. The filesystem doesen't need to know or understand the content type of it's files to traverse them.

_Imagine a file system where you didn't have generic files but typed word, excel, etc. documents (like we have typed classes in our code); you would then need different programs to transfer each file type over the network (wordftp, excelftp, etc.) and in contrast wouldn't it be wonderful if we could open and save all file formats with every application instead? Well now it's possible, with the node-graph!_

<p align='center'><img src='http://sprout.googlecode.com/files/poll.gif' /></p>


---


And performance is not a problem, take the example below where we wanted a complex search that we dreaded to implement in mysql with innodb.

_The query is a search on article -body (201), -title (200) and username (100), that returns a list of articles ordered by date. The tricky part is the JOIN hierarchy_

_1 second with 10000 rows_
```
return "FROM node n, meta m1, data d1, data d2, link l1, node n2, meta m2, data d3 " + 
"WHERE ((d1.type = 200 AND d1.value LIKE '%" + query + "%') OR " + 
"(d2.type = 201 AND d2.value LIKE '%" + query + "%') OR " + 
"(d3.type = 100 AND d3.value LIKE '%" + query + "%')) AND " + 
"(n.id = m1.node AND m1.data = d1.id AND m1.data = d2.id AND n.id = l1.parent AND l1.type = " + 
(ARTICLE | USER) + " AND l1.child = n2.id AND n2.id = m2.node AND m2.data = d3.id)";
```


---


We decided to build our node graph on top of a traditional RDBMS and ORM which has its pro's and con's: The advantage is that you can extract data in a straight forward way with SQL and that you get simple indexing for free, the drawback is that depth iteration can be costly, but not that costly as we saw in the example above.

The reason is simple: there are no Node-Graph databases with FULL DATA INDEX and JOIN select capability.

Until then my tests shows that the LIKE statement is going to cause alot more problems than the JOIN hierarchy because of full text search still not being implemented in innodb, so for larger projects you will need to either mirror the data table with a myisam table to search on or implement an external indexing engine like lucene.

**Update**: Added a poll table, so that you can sort nodes by any number. This is good for; well polls, among other things.

**Update**: Changed the data.value type to blob, this allows us to store text as before without collation issues AND now we can store binary data without Base64 encoding it first.