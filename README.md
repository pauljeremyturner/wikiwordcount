# Instructions

## Running the Executable Jar File
The application jar file can be found at `<project root>/build/libs/wikiwordcount-0.1.0.jar`.

There are 2 'modes' to operate the solution.  These are `calculate` and `select`.  Calculate mode stores the word counts in a mongo database and select mode retrieves them

### Calculate Mode

There is only 1 mandatory file parameter - the absolute path to the wiki dump file;

here is an example

```
java -jar ./wikiwordcount.jar calculate --source /home/paul/git-repositories/github.com/pauljeremyturner/wikiwordcount/src/main/resources/enwiki-20190101-pages-articles-multistream.xml
```

Default used here include: localhost mongodb server on default port 27017
The file is read and processed in 2GB chunks by default.

Here are the full options available

`--word-digest-threads` How many threads in the pool for extracting word counts from file chunks - default 8

`--off-heap` true Whether to use off heap memory for byte buffers or not - default true

`--chunk-size` How many  bytes to reserve for a chunk to be processed in parallel - default 2G (can use B, K, M, G suffixes to denote bytes, kilobytes, megabytes and gigabytes respectively)

`--mongo-server` host and port for mongodb connections - default localhost:27017

Here is an example:


```
--file-path /home/paul/git-repositories/github.com/pauljeremyturner/wikiwordcount/src/main/resources/enwiki-20190101-pages-articles-multistream.xml --word-digest-threads 8 --off-heap true --chunk-size 2G --mongo-server 172.17.0.2:27017
```

### Select Mode

There is only 1 mandatory file parameter - the absolute path to the wiki dump file;

here is an example

```
java -jar ./wikiwordcount.jar select --count 50 --word-length --source /home/paul/git-repositories/github.com/pauljeremyturner/wikiwordcount/src/main/resources/enwiki-20190101-pages-articles-multistream.xml
```

 ### Available Options
 The

`--count` How many results to show.  Default is 20

`--direction` Whether to show to most (DESC) or least popular (ASC) words.  Default is DESC

`--word-length` Show statistics for words only with the specified length.  Default is not to filter by word length

Here is an example of the Select mode in action:

```
SelectOptions:: [mongo uri=172.17.0.2:27017] [sort-direction=DESC] [word-count number=20] [word-length=7]

```

### Why So Many Options
I needed to run this on a Xeon Desktop and an i5 Laptop during the easter break and I wanted to be able to test the extent to which the solution is capable of using the available resources.
I need to tune some components to ork on smaller datasets for unit tests
I thought the available options were useful

## How Does This Work?

The solution only generates string objects for genuine words checked against a dictionary.  No strings are created for
lines of the file or any intermediate chunks of the file.
File I/O is done via nio SeekableChannels and RandomAccessFiles.

### Parrelisation

The solution attempts to run as many operations in parallel as possible.

Done sequentially:
Split file into arbitrary chunks and store start/end indexes in mongodb
Mark a processing chunk as reserved for procesing in mongodb.

Done in parallel
Read a processing chunk into a buffer and split into subchunks
Perform a word count for each subchunk
Store word counts in mongodb

On my 16 core Xeon System, virtually all cores were maxed out during the file processing.  Using 1Gb file chunks there was a slight drop in processor activity
following completing of the previous chunk and reading of the next one.

### Conflicts Among Multiple Participating JVMs
Each JVM participating in the word count

1: Create a file 'hash' string to ensure working on same file as peers
2: Read an arbitrary chunk of the file into memory (either on or off heap memory)
3: trim that chunk, discarding the end part that is not a whole <page>
4: report the actual chunk start/end bytes as reserved with a timestamp in case the jvm died
5: split the chunk and perform a parallel word count of chunk and store word count digest in mongo

This produces word counts in mongo that can be aggregated using a map-reduce function

if 2 JVMs start working on the same area of the file at the same time, the file chunks being processed and the resultant word counts are stored in mongo according to the dump file, its filesize in bytes and the chunk size in bytes.
This means that only word counts for the same file being processed in the same way will be stored together.
If all JVMs working on the file have the same chunk size set, there will be a deterministic set of chunks produced by the algorithm to divide the file.
Each chunk is reserved to ensure that 2 process to not work on the same part of the file, but if a race condition allowed this to happen (it's possible)
then an exception will be thrown when the chunk is attempted to be written for a second time.

### Failure in a Participating JVM


# Glossary of Terms
I have tried to use the same terminology throughout the code and in this readme.  Here are some definitions for clarification:

`ProcessingChunk` - This represents a dhunk of the dump file that will ultimately be split up and processed in parallel.  The default size is 1 GB

`Probe Chunk` - This represents a small chunk of the dump file that is read only to determine where a <page> starts and ends.  There is no attempt to process the dump file as xml,
however because the file is read in bytes, it is important not to split a multibyte UTF-8 character as this will upset the process that converts from bytes to characters.
This is based on the idea that it is not necessary to read the whole file (even if it fit in memory) to determine where the prcessing chunks should start and end.

`Subcunk` - This represents a chunk of the file that is prccessed as characters and is subject to word count via a `CompletableFuture`

`Hijack` - This involves a JVM starting to perform a word count on a processing chunk that has already been reserved by another JV because it has taken too long to process it
or because the JVM has died after reserving the chunk but before completing the word count and storing it in mongodb.

`Chunk Digest` - This represents word count data from a processing chunk.

## MongoDB
I used a dockerised version of mongo


## Improvements
If I had more time to do the exercise, I would:
1: Use MongoDB sharding and map/reduce function instead of selecting and merging on the java side.
2: I only had time for tests that were essential for rudimentary tdd.  Of course integration tests should be added and a better coverage of unit tests.
3: File subchunks are processed in parallel, however another processing chunk is only started after all subchunks of the previous processing chunk have been processed.
There is an opportunity to read and process processing chunks in a threadpool.  My worry was that if this were the case, the first JVM to start would reserve all the
processing chunks and not let another JVM process anything.

I found the torrent an easier way to get the dump than the direct download:
https://itorrents.org/torrent/B4531B9C5337B0B078CE8D8EEC3F8E83B2AEA583.torrent


