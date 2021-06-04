# A Map with Expiring Entries

## Summary
The <a href=https://github.com/dchampion/expiring-entry-map/blob/main/src/main/java/com/dchampion/ExpiringEntryMap.java target="_blank">single class in this repository</a> is an implementation of the <code>java.util.Map</code> collection. It contains entries that expire (i.e., that are evicted) after a user-defined period of time.

For an explanation of the purpose and features of this class, jump to a full [Description of the Project](#Description-of-the-Project) below.

## Requirements
To run this project, the Java 8+ runtime environment must be installed on your computer (this will be the default case on most modern computers).

## Download the Project
To run this project, you must first download it from this repository.
* If <code>Git</code> is installed, navigate to a clean filesystem directory using a command-line shell and type <code>git clone https<nolink>://github.com/dchampion/expiring-entry-map.git</code>

* If <code>Git</code> is not installed, or you do not wish to use it, click the <code>Code</code> button at the top of this page and select <code>Download ZIP</code>. Then extract the contents of the downloaded ZIP file to a clean filesystem directory.

## Build and Run the Project
Using a command-line shell, navigate to the directory <code>expiring-entry-map</code> and type one of the following commands, depending on your computer's operating system:
* If Windows, type <code>mvnw clean install</code>

* If not Windows (e.g., a system such as MacOS, or any of a variety of Linux distributions), type <code>./mvnw clean install</code>

This command will build the project and run a <a href=https://github.com/dchampion/expiring-entry-map/blob/main/src/test/java/com/dchampion/ExpiringEntryMapTest.java>suite of unit tests</a> that demonstrate both the correctness and functionality of the <a href=https://github.com/dchampion/expiring-entry-map/blob/main/src/main/java/com/dchampion/ExpiringEntryMap.java target="_blank">source code</a>.

## Description of the Project
Yeah, I know, this has been done before; notably <a href=https://github.com/google/guava/wiki/CachesExplained>here</a> (and likely a hundred other places). And no doubt it's been done better (how is a mere dilletante to stand a chance against an army of Google engineers?).

I'm all for not reinventing the wheel. But when I first wrote this class back in June 2011 the Google version hadn't been published yet, and I needed a Map with expiring entries for a project I was working on at the time.

In a pang of pandemic boredom, I have resurrected said Map here, and done so without studying the Google implementation in advance (that would have been cheating). However, I did make a few changes to my original version, which contained flaws that in retrospect seem naive.

## Likely Critiques, and Defenses Thereof
* <b>Critique #1</b>: You are using the raw type <code>java.util.Map</code> to wrap the user-supplied implementation.

* <b>Defense #1</b>: Guilty as charged (but I used <code>@SuppressWarnings</code> liberally to shut the compiler up...just kidding!). This was a tough one. Compile-time type checking has saved many a programmer from him/herself. But there are exceptions to even the most strident rules, and I made one here for the following reasons:

    First, the class of which the raw type&mdash;a <code>java.util.Map</code>&mdash;is a member is final, and the member itself private. Therefore, it is inaccessible to any code outside of the class. This means nobody but I can do the wrong thing with it.

    Second, for my approach to work (defense of that forthcoming), I need to put values into the raw Map that are of a different type than that specified by the client when he/she created the underlying Map. Putting type constraints on the raw Map would have forbidden this.

* <b>Critique #2</b>: Instead of wrapping the Map's values to support timed entries, wouldn't wrapping its entries have been a better abstraction?

* <b>Defense #2</b>: Yes, semantically, timed <i>entry</i> makes more sense than timed <i>value</i>&mdash;it is called Expiring<i>Entry</i>Map, after all. However, every Map implementation contains a fit-for-purpose and well-hidden <code>Map.Entry</code> implementation. Wrapping <code>Map.Entry</code> instances, instead of value instances, would have required the services of a complex and heavyweight proxy library (<a href=https://github.com/cglib/cglib/wiki>CGLib</a>, for example).

    Piggybacking a timestamp onto a Map's value, on the other hand, provides a zero-visibility (from the Map's perspective) vector for pairing the requisite metadata with a Map's entries, and doesn't in any way disturb the Map's implementation. Wrapping <code>Map.Entry</code> would have made my implementation far more complex; and complexity is the enemy of correctness.

 * <b>Critique #3</b>: You haven't addressed serialization.

 * <b>Defense #3</b>: I am aware of this. I will address it as time/inclination permits.