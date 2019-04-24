# Instructions

These instructions enable the reader to repeat the same tests I have carried out to demonstrate this fully working solution.

## Test Environment

* Linux

```
Fedora (4.18.16-300.fc29.x86_64)
```

* Dump File

```
enwiki-20190101-pages-articles-multistream.xml.bz2 (16693557478 Bytes)

https://itorrents.org/torrent/B4531B9C5337B0B078CE8D8EEC3F8E83B2AEA583.torrent
https://meta.wikimedia.org/wiki/Data_dump_torrents
```

* Oracle Java 8

```
java version "1.8.0_181"
Java(TM) SE Runtime Environment (build 1.8.0_181-b13)
Java HotSpot(TM) 64-Bit Server VM (build 25.181-b13, mixed mode)
```

* MongoDb

```
mongo:latest` docker image
CONTROL  [initandlisten] db version v4.0.9
```

## Running the Executable Jar File
The application jar file can be found at `<project root>/build/libs/wikiwordcount-0.1.0.jar`.

There are 2 'modes' to operate the solution.  These are `calculate` and `select`.  Calculate mode stores the word counts in a mongo database and select mode retrieves them.

In order to view word counts, you need to run calculate then select.  You can run select before calculate has finished, it will tell you the results are incomplete.

### Calculate Mode

There is only 1 mandatory file parameter - the absolute path to the wiki dump file;

```
java [JVM options] -jar  ./wikiwordcount.jar calculate --source [dump file]
```

Here is an example

```
java -Xmx8192M -jar ./wikiwordcount.jar calculate --source /home/paul/git-repositories/github.com/pauljeremyturner/wikiwordcount/src/main/resources/enwiki-20190101-pages-articles-multistream.xml
```

Defaults used here include: localhost mongodb server on port 27017
The file is read and processed in 2GB chunks by default.

Here are the full options available

`--chunk-size` How many  bytes to reserve for a chunk to be processed in parallel - default 1G (can use B, K, M, G suffixes to denote bytes, kilobytes, megabytes and gigabytes respectively)

`--mongo` host and port for mongodb connections - default localhost:27017

Here is an example:


```
cd <project-root>
cd build/libs
java -Xmx8192M -jar ./wikiwordcount-0.1.0.jar calculate --source /home/paul/git-repositories/github.com/pauljeremyturner/wikiwordcount/src/main/resources/enwiki-20190101-pages-articles-multistream.xml --chunk-size 2G --mongo-server 172.17.0.2:27017
```

Recommended heap size is 8GB.  The live data objects size during a run was observed to be about 4GB.
GC should be tuned for throughput as latency is not important here.  I haven't done this.

### Select Mode

There is only 1 mandatory file parameter - the absolute path to the wiki dump file;


```
java [JVM options] -jar  ./wikiwordcount.jar select --source [dump file]
```

here is an example

```
cd <project-root>
cd build/libs
java -Xmx8192M -jar ./wikiwordcount-0.1.0.jar select --source /home/paul/git-repositories/github.com/pauljeremyturner/wikiwordcount/src/main/resources/enwiki-20190101-pages-articles-multistream.xml
```

 ### Available Options
 These options attempt to address te 'statistically interesting words' part of the exercise.

`--count` How many results to show.  Default is 20

`--sort-direction` Whether to show to most (DESC) or least popular (ASC) words.  Default is DESC

`--word-length` Show statistics for words only with the specified length.  Default is not to filter by word length

`--chunk-size`   This is only necessary if non-default values used in calculate stage.  This is because the chunk size is used in mongo ids in order to differentiate chunks with different sizes in case 2 processes for the same file but with different chunk size were running at the same time.

`--mongo` host and port for mongodb connections - default localhost:27017

Here is an example of the Select mode in action:

```
java -Xmx8192M -jar ./wikiwordcount-0.1.0.jar select --count 50 --word-length 8 --source /home/paul/git-repositories/github.com/pauljeremyturner/wikiwordcount/src/main/resources/enwiki-20190101-pages-articles-multistream.xml

```

## How Does This Work?

The solution only generates string objects for genuine words checked against a dictionary.  No strings are created for
lines of the file or any intermediate chunks of the file.
File I/O is done via nio SeekableChannels and RandomAccessFiles.

### Parrelisation (?)

The solution attempts to run as many operations in parallel as possible.

**Done sequentially:**

* Split file into arbitrary chunks and store start/end indexes in mongodb

* Mark a processing chunk as reserved for procesing in mongodb.

**Done in parallel**

* Read a processing chunk into a buffer and split into subchunks

* Perform a word count for each subchunk

* Store word counts in mongodb

On my system, virtually all cores were maxed out during the file processing.  Using 1Gb file chunks there was a slight drop in processor activity
following completing of the previous chunk and reading of the next one.

### Conflicts Among Multiple Participating JVMs

Each JVM participating in the word count

1: Create a file 'hash' string to ensure working on same file as peers

2: Read an arbitrary chunk of the file into memory

3: trim that chunk, discarding the end part that is not a whole <page>

4: report the actual chunk start/end bytes as reserved with a timestamp in case the jvm died

5: split the chunk and perform a parallel word count of chunk and store word count digest in mongo

This produces word counts in mongo that can be aggregated using a map-reduce function - either by mongo db map-reduce or by anoth erJava process

If 2 JVMs start working on the same area of the file at the same time, the file chunks being processed and the resultant word counts are stored in mongo according to the dump file, its filesize in bytes and the chunk size in bytes.
This means that only word counts for the same file being processed in the same way will be stored together.

If all JVMs working on the file need to have the same chunk size set, there will be a deterministic set of chunks produced by the algorithm to divide the file.


### Failure in a Participating JVM

This is tolerated.

When a processing chunk is marked as reserved 'processing' no other process will attempt to process it until no more not 'processing' chunks remain.  When all unreserved processing chunk have been completed, chunks
that have been reserved by a process that has failed will become eligible for processing.


# Glossary of Terms
I have tried to use the same terminology throughout the code and in this readme.  Here are some definitions for clarification:

`ProcessingChunk` - This represents a dhunk of the dump file that will ultimately be split up and processed in parallel.  The default size is 1 GB

`Probe Chunk` - This represents a small chunk of the dump file that is read only to determine where a <page> starts and ends.  There is no attempt to process the dump file as xml,
however because the file is read in bytes, it is important not to split a multibyte UTF-8 character as this will upset the process that converts from bytes to characters.
This is based on the idea that it is not necessary to read the whole file (even if it fits in memory) to determine where the prcessing chunks should start and end.

`Subcunk` - This represents a chunk of the file that is prccessed as characters and is subject to word count via a `CompletableFuture`

`Hijack` - This involves a JVM starting to perform a word count on a processing chunk that has already been reserved by another JVM because it has taken too long to process it
or because the JVM has died after reserving the chunk but before completing the word count and storing it in mongodb.

`Chunk Digest` - This represents word count data from a processing chunk.

### Logging

* Calculate Command

Here is an extract from the calculate command showing chunks being processed in fork join pool.

```
2019-04-22 20:50:26.219 [main] INFO  c.p.w.command.CalculateCommand - Processing a chunk of dump file [chunk #=51]
2019-04-22 20:50:26.219 [main] DEBUG c.p.wikiwordcount.io.ByteBufferPool - Acquired ByteBuffer from pool [byteBuffer identityHashCode=496729294]
2019-04-22 20:50:31.019 [main] INFO  c.p.w.command.FileChunkWorker - Split a chunk in to subchunks and started extracting words [chunk=ProcessingChunk:: [start=55836020880], [end=56909772260], [index=52]] [subchunk count=8]
2019-04-22 20:50:53.840 [ForkJoinPool.commonPool-worker-1] INFO  c.p.w.command.FileChunkWorker - Save ChunkDigest [index=52]
2019-04-22 20:50:54.310 [main] DEBUG c.p.wikiwordcount.io.ByteBufferPool - Returned ByteBuffer to pool [byteBuffer identityHashCode=496729294]
2019-04-22 20:50:54.310 [main] INFO  c.p.w.command.FileChunkWorker - Completed word count of all subchunks [chunk=ProcessingChunk:: [start=55836020880], [end=56909772260], [index=52]]
2019-04-22 20:50:54.310 [main] INFO  c.p.w.command.CalculateCommand - Processing a chunk of dump file [chunk #=13]
2019-04-22 20:50:54.310 [main] DEBUG c.p.wikiwordcount.io.ByteBufferPool - Acquired ByteBuffer from pool [byteBuffer identityHashCode=496729294]
2019-04-22 20:50:58.145 [main] INFO  c.p.w.command.FileChunkWorker - Split a chunk in to subchunks and started extracting words [chunk=ProcessingChunk:: [start=13958892791], [end=15032641180], [index=13]] [subchunk count=8]
2019-04-22 20:51:26.460 [ForkJoinPool.commonPool-worker-0] INFO  c.p.w.command.FileChunkWorker - Save ChunkDigest [index=13]
2019-04-22 20:51:26.728 [main] DEBUG c.p.wikiwordcount.io.ByteBufferPool - Returned ByteBuffer to pool [byteBuffer identityHashCode=496729294]
2019-04-22 20:51:26.728 [main] INFO  c.p.w.command.FileChunkWorker - Completed word count of all subchunks [chunk=ProcessingChunk:: [start=13958892791], [end=15032641180], [index=13]]
```

* Select Command

Here is an extract from the select command log showing the top 20 occurring words,  ignoring word length.

```
2019-04-22 23:43:16.268 [main] INFO  c.p.w.command.SelectCommand - {
  "the" : 307190762,
  "of" : 183657786,
  "and" : 125966196,
  "to" : 108059767,
  "a" : 106500729,
  "ref" : 102045461,
  "for" : 49844180,
  "category" : 49377253,
  "name" : 46717208,
  "com" : 45675723,
  "title" : 45462619,
  "on" : 43936421,
  "was" : 40345056,
  "as" : 38352953,
  "by" : 36732859,
  "talk" : 36495017,
  "date" : 35983033,
  "with" : 32005415,
  "from" : 29828115,
  "that" : 29739130
}

```

## Improvements

If I had more time to do the exercise, I would:

1: Use MongoDB sharding and map/reduce function instead of selecting and merging on the java side.

2: I only had time for tests that were essential for rudimentary tdd.  Of course integration tests should be added and a better coverage of unit tests.

3: File subchunks are processed in parallel, however another processing chunk is only started after all subchunks of the previous processing chunk have been processed.
There is an opportunity to read and process processing chunks in parallel.


