Changeset1
==========
Process OSM's changeset feed and check node overlaps within a bounding box

What it's not designed to be:
A replacement for the OSM History Tab, OWL, WhoDidIt, or any of the tools described on http://wiki.openstreetmap.org/wiki/Quality_assurance

What it is:
A small java program that can be invoked per user or with a file containing a list of command-line arguments allowing edits by particular users within particular areas to be monitored.

It uses the OSM API so don't go mad with it - follow http://wiki.openstreetmap.org/wiki/API_usage_policy


Eclipse
-------
You can import it into Eclipse if you want to (although, given that it's only one Java file `Changeset1.java`, there's really no need to).  If you're not using an IDE, install a JDK (e.g. from http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html), then just ensure that the `javac` that you have just installed is on the PATH and `javac Changeset1.java` in order to create `Notes01.class`.


Usage examples
--------------
    java Changeset1 -time="2013-11-04T20:53" -debug=5 -display_name="SomeoneElse" -bbox=-2.123,52.809,-0.331,53.521 -output=example_out.txt

This looks for changesets by the user named `SomeoneElse` in the sepecified bounding box.  The output file contains lines like this, one per changeset:

<pre>
SomeoneElse;61942;18740137;Potlatch 2;2.3;Wragby road - updated lanes where I'd miscounted.;Changeset: bbox overlaps
</pre>

Pass a `download` parameter and it'll download changeset contents and check for things that might be iffy in there too:

    java Changeset1 -time="2013-11-04T20:53" -debug=5 -display_name="SomeoneElse" -bbox=-2.123,52.809,-0.331,53.521 -download=1 -output=example_out.txt


You can also group a series of checks into an input file.  Let's imagine that `example2_in.txt` contains:

<pre>
-display_name="SomeoneElse_Revert" -bbox=-7,50,2,61 -download=1
-display_name="SomeoneElse"  -bbox=-2.123,52.809,-0.331,53.521 -download=1
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

