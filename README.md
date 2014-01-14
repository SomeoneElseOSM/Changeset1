Changeset1
==========
Process OSM's changeset feed, and check node overlaps within a bounding box

What it's not designed to be:
A replacement for the OSM History Tab, OWL, WhoDidIt, or any of the tools described on http://wiki.openstreetmap.org/wiki/Quality_assurance

What it is:
A small java program that can be invoked per user or with a file containing a list of command-line arguments allowing edits by particular users within particular areas to be monitored.

It uses the OSM API so please don't go mad with it - follow http://wiki.openstreetmap.org/wiki/API_usage_policy


Eclipse
-------
You can import it into Eclipse if you want to (although, given that it's only two Java classes `Changeset1` and `OsmObjectInfo`, there's really no need to).  If you're not using an IDE, install a JDK (e.g. from http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html), then just ensure that the `javac` that you have just installed is on the PATH and `javac Changeset1.java` and `javac OsmObjectInfo.java` in order to create `Changeset1.class` and `OsmObjectInfo.class`.


Usage examples
--------------
    java Changeset1 -time="2013-11-04T20:53" -debug=5 -display_name="SomeoneElse" -bbox=-2.123,52.809,-0.331,53.521 -output=example_out.txt

This looks for changesets by the user named `SomeoneElse` in the specified bounding box since the specified time.  The output file contains lines like this, one per changeset:

<pre>
SomeoneElse;61942;18740137;Potlatch 2;2.3;Wragby road - updated lanes where I'd miscounted.;Changeset: bbox overlaps
</pre>

(that's username, userid, changeset number, editor version, changeset comment and the reason that it's in the list, all comma-separated).

Pass a `download_changeset` parameter and it'll download changeset contents and check for things that might be iffy in there too:

    java Changeset1 -time="2013-11-04T20:53" -debug=5 -display_name="SomeoneElse" -bbox=-2.123,52.809,-0.331,53.521 -download_changeset=1 -output=example_out.txt


Pass a `building` parameter as well as `download_changeset` and it'll check for buildings and shops with more than a certain number of nodes.

    java Changeset1 -time="2013-11-04T20:53" -debug=5 -display_name="SomeoneElse" -bbox=-2.123,52.809,-0.331,53.521 -download_changeset=1 -building=10 -output=example_out.txt

This is designed to help flag landuse areas that have been changed to buildings or shops by iD users by mistake (see https://github.com/systemed/iD/issues/542).


You can also group a series of checks into an input file.  Let's imagine that `example2_in.txt` contains:

<pre>
-display_name="SomeoneElse_Revert" -bbox=-7,50,2,61 -download_changeset=1
-display_name="SomeoneElse"  -bbox=-2.123,52.809,-0.331,53.521 -download_changeset=1
</pre>

You can then:

    java Changeset1 -time="2013-11-04T20:53" -debug=5 -input=example1_in.txt -output=example2_out.txt

It'll then process each of the users in the input file.

The maximum number of changesets returned is restricted by the API limit.


Things that it can spot
-----------------------
* Deleted ways and relations
* Single node ways
* Ways with no tags (which then need to be checked manually to make sure that they're not part of a relation, or were updated later in the changeset to have tags).
* Landuse ways that have been erronously converted to buildings or shops because of iD bug 542.


Supported Parameters
---------------------
### -help  
Prints a basic "usage" screen.

### -input=some_input_file.txt
Specifies an input file containing lines of users to check (and other parameters).

### -output=some_output_file.txt
Specifies an output file into which a summary of findings will be written.

### -display_name="Some User Name"
Specifies a user's display name to search for changesets for.  It will be URLencoded before being passed to the API

### -user=112
Specifies a user's userid to search for changesets for.  Useful for when display names change, or when they contain characters that can't easily be passed from the command line.

### -time="2013-11-04T20:53"
The time, specified in a way that the API will understand, to search for changesets from.

### -dev
Use the dev server (api06.dev.openstreetmap.org) instead of the live one.
 
### -debug=0
A number between 0 and 8, used to control the amount of debug written to stdout as processing occurs.  The higher the number, the more debug.

### -bbox=-2.123,52.809,-0.331,53.521
The bounding box to check changesets against.  If specified it's not passed to the API, so the output will show "X changesets, none of interest" if a mapper has mapped elsewhere.  If "-download_changeset=1" is also specified, then nodes in a changeset will be checked for overlap.  If "-download_nodes=1" is also specified, way nodes will also be checked.

### -download_changeset=1
Download each changeset and check for further potential issues (e.g. single- or zero-node ways, ways without tags, etc.) 

### -download_nodes=1
Also individually download nodes for ways in a changeset.  Recommended only for very small changesets.
 
### -building=14
If "-download_changeset=1" is specified and this parameter is too, buildings that iD users might have inadvertantly converted from residential areas will be reported if they contain more than this number of nodes. 

### -report_overlap_nodes=1
Attempt to report all nodes in a changeset that overlap the bounding box, not just the fact that some nodes overlap.  Requires that "-download_changeset=1" is also set.


Finally
-------
It's still very much a work in progress - there is lots of tidying up that remains to be done.
