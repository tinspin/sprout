To host a rupy/sprout site you need a server provider that can meet the following [hosting](http://code.google.com/p/sprout/wiki/Hosting) requirements.

Singel _was a valentines day campaign site for Swedish match-making site [Spraydate](http://www.spraydate.se)._

  * Production: Three people for one week full-time.
  * Statistics: ~10000 pageviews and ~5 GB of data transfered; per day.
  * Performance: CPU usage on server is average 2.14% of which most is MySQL, heap footprint is ~1 MB.

> While fairly simple and without heavy load, this pilot project has proved the speed, agility and robustness of sprout in a commercial production with short deadline.

[Hemlistan](http://www.hemlistan.se) _is a real-estate scraper/spider/crawler that aggregates swedens monopoly housing site [Hemnet](http://www.hemnet.se)._

  * Production: Two man weeks.

> The production value on this project is high, it took me one week to build something that others have built companies around: [Bovision](http://www.bovision.se), [Booli](http://www.booli.se), [Boliga](http://www.boliga.se).

Pixboom _was an image tagging solution._

  * Production: Three people for a month full-time.
  * Statistics: ~40000 hits per day and growing.
  * Performance: Load average is 0.0% on an AWS small EC2.

[Huxville](http://www.huxville.com) _is a MMO game for kids._

  * Runs rupy 1.1 standalone on a cluster of 10 independent servers.
  * Each node serves the ~50MB large flash 3D client to ~200 concurrent users with ~10% CPU as fast as the customers connection can deliver it with a AWS micro instance.
  * **CPU IO wait is _none_ for the rupy process.**
  * **Memory usage is really low ~64MB of which ~1MB heap and doesn't leak.**
  * **Neither file or socket FD leak.**

  * _Rupy is now guaranteed stable._

[BitcoinBankBook](http://bitcoinbankbook.com) _GWT bitcoin bank._

  * Runs on a Raspberry Pi.
  * Production: One man week.

[Aeon Alpha](http://aeonalpha.com) _Realtime Multiplayer Online Space Shooter._

  * Compatible with Oculus Rift DK1 and DK2.
  * Runs on a cluster of Raspberry Pi's.
  * Production: 1/2 man years.