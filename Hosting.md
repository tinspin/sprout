<font color='red'><b><i>NEW</i></b></font> There are FREE raspberry pi colocations popping up everywhere, rupy/sprout runs really well on these!

| Country | Speed | Volume | Comment | Date |
|:--------|:------|:-------|:--------|:-----|
| [Austria](http://www.edis.at/en/server/colocation/austria/raspberrypi) | 100Mb | 100GB | Out of stock | 5/11-14 |
| [Netherlands](http://raspberrycolocation.com) | 100Mb | 500MB | Out of stock | 5/11-14 |
| [Sweden](https://fsdata.se/server/raspberry-pi-colocation) | 1Mb | ~ | Out of stock | 5/11-14 |

<font color='orange'><b><i>NEW</i></b></font> Try our hosting solution: [host.rupy.se](http://host.rupy.se)!

<font color='lightblue'><b><i>NEW</i></b></font> Rupy works great on the AWS (Amazon Web Services) micro instance.

There are 2 things your hosting has to provide and that you need to know how to use for you to be able to take advantage of sprout:

  1. Java (1.5 or later)
  1. MySQL, PostgreSQL or Oracle (any versions)

The easiest way to get this cheaply is to buy a Virtual Private Server (VPS) solution:

| Company | Price | Volume | Space | Country | Date |
|:--------|:------|:-------|:------|:--------|:-----|
| [Glesys](http://glesys.se/serverhotell/vps.php) | 79 SEK | 50 GB | 5 GB | Sweden | 10/4-09 |
| [IPeer](http://www.ipeer.se/vps.php) | 165 SEK | 350 GB | 15 GB | Sweden | 10/4-09 |
| [Strongbox](http://www.strongbox.se/vps.php) | 165 SEK | 250 GB | 15 GB | Sweden | 12/4-09 |
| [Levonline](http://www.levonline.com/sv/server-hotell.html) | 250 SEK | 20 GB | 10 GB | Sweden | 10/4-09 |
| [Serverchoice](http://www.serverchoice.com/vps) | £15 | 50 GB | 5 GB | UK | 10/4-09 |
| [Demon](http://www.demon.net/demon/products/hosting/virtualserverhosting) | £30 | 15 GB | 500 GB | UK | 10/4-09 |
| [Clustered](http://www.clustered.net/vds/) | £35 | 1 TB | 50 GB | UK | 10/4-09 |
| [Solidhost](http://www.solidhost.com/products/linux-vps/) | £39 | 200 GB | 10 GB | UK | 10/4-09 |
| [Slicehost](http://www.slicehost.com) | 20$ | 150 GB | 10 GB | USA | 17/11-10 |
| [Knownhost](http://knownhost.com/) | 20$ | 125 GB | 7 GB | USA | 10/4-09 |
| [PowerVPS](http://www.powervps.com/no-control-panel-vps.php) | 40$ | 500 GB | 10 GB | USA | 10/4-09 |
| [Hostforweb](http://www.hostforweb.com/vps/index.php) | 40$ | 400 GB | 15 GB | USA | 10/4-09 |
| [SolarVPS](http://www.solarvps.com/linux-vps.php) | 40$ | 300 GB | 15 GB | USA | 10/4-09 |
| [WiredTree](http://www.wiredtree.com/managedvps/) | 44$ | 600 GB | 40 GB | USA | 10/4-09 |

It's recommended that you use linux ~~and front the sprout/rupy server with an Apache~~, so then it makes sense if you know:

  1. SSH
  1. ~~Apache (with mod\_proxy)~~
  1. ~~HAProxy, to loadbalance or proxy your site.~~

Observe that OpenVZ does have it's fair share of issues with virtual memory and therefore you will not be able to compile on the host machine; so it's great that rupy can hot deploy directly to the host from the development machine remotely via HTTP.

~~If you want to host multiple sites on one machine or host one site on multiple machines, you have to [VirtualHostProxy](http://code.google.com/p/rupy/wiki/VirtualHostProxy) or loadbalance your domain with Apache or HAProxy.~~

NEW: Rupy now has hosted mode!

**Dependencies:** _Sprout uses [rupy](http://rupy.googlecode.com), a "tiny footprint" embedded HTTP server that uses below 1 MB in heap, it's not a problem to run one rupy instance per 32 MB of RAM, which means that on any VPS account you will be able to run at least 4 instances, since the smallest amount of guaranteed RAM is 128 MB in the solutions above._
