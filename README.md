# Instructions

## Executable Jar File
The application jar file can be found at `build/libs/wikiwordcount.jar`.

There are 2 'modes' to operate the solution.  These are calculate and select.  Calculate mode stores the word counts in a mongo database and select mode retrieves them

### Calculate Mode

There is only 1 mandatory file parameter - the absolute path to the wiki dump file;

here is an example

```
java -jar ./wikiwordcount.jar calculate --source /home/paul/git-repositories/github.com/paul.jeremy.turner/wikiwordcount/src/main/resources/enwiki-20190101-pages-articles-multistream.xml 
```

Default used here include: localhost mongodb server on default port 27017
The file is read and processed in 2GB chunks by default.

Here are the full options available

```
--file-path /home/paul/git-repositories/github.com/paul.jeremy.turner/wikiwordcount/src/main/resources/enwiki-20190101-pages-articles-multistream.xml --word-digest-threads 8 --off-heap true --chunk-size 2G --mongo-server 172.17.0.2:27017
```

`--word-digest-threads` How many threads in the pool for extracting word counts from file chunks - default 8

`--off-heap` true Whether to use off heap memory for byte buffers or not - default true

`--chunk-size` How many  bytes to reserve for a chunk to be processed in parallel - default 2G (can use B, K, M, G suffixes to denote bytes, kilobytes, megabytes and gigabytes respectively)

`--mongo-server` host and port for mongodb connections - default localhost:27017


### Select Mode

There is only 1 mandatory file parameter - the absolute path to the wiki dump file;

here is an exampl,e

```
java -jar ./wikiwordcount.jar select --count 50 --word-length --source /home/paul/git-repositories/github.com/paul.jeremy.turner/wikiwordcount/src/main/resources/enwiki-20190101-pages-articles-multistream.xml
```
 
Here are the full options available

`--count` How many results to show.  Default is 20

`--direction` Whether to show to most (DESC) or least popular (ASC) words.  Default is DESC

`--word-length` Show statistics for words only with the specified length.  Default is not to filter by word length


## How Does This Work?

My solution only generates string objects for genuine words checked against a dictionary.  No strings are created for
lines of the file or any intermediate chunks of the file.  
File I/O is done via nio SeekableChannels and RandomAccessFiles.
Reserving file chunks and starting to process them is done single threaded and sequentially, processing file chunks for word counts is done in parallel.
The first step needs to be single threaded because otherwise the entire file will be reserved and a thread pool will fill up with a huge number of tasks.
On my 16 core Xeon System, virtually all cores were maxed out during the file processing.  Using 1Gb file chunks there was a slight drop in processor activity
following completing of the previous chunk and reading of the next one. 

A large chunk is chosen from the file based on what is stored in mongoDb to represent chunk of the file that have already been reserved.  

Once a chunk has been selected, it is trimmed to include whole lines only (otherwise decoding from bytes into characters may fail if a multibyte character is chopped)
and the chunk reserved in mongodb with a timestamp so that the chunk can be picked up by another process if it takes too long.

Then, the reserved chunk is processed in parallel by dividing into smaller sub-chunks and checking each string sequence against dictionary words and the word count adjusted accordingly.
This uses a Fork-Join pool and Completable Future api.


Each JVM participating in the Parallel Word Occurrence Count 

1: Create a file checksum string to ensure working on same file as peers
2: Read an arbitrary chunk of the file into memory (either on or off heap memory)
3: trim that chunk, discarding the end part that is not a whole <page>
4: report the actual chunk start/end bytes as reserved with a timestamp in case the jvm died
5: split the chunk and perform a parallel word count of chunk and store word count digest in mongo

This produces word counts in mongo that can be aggregated using a map-reduce function

Physical Structure (ByteBuffer chunks)
```
 _______________________________________________
|___________|___________|___________|___________|
```
Logical Structure (<page> tags)
```
 _______________________________________________
|__|__|__|__|__|__|__|_||__|__|_||__|__|__|__|_|
```

What Happens if 2 JVMs start working on the same area of the file at the same time?
If all JVMs working on the file have the same chunk size set, there will be a deterministic set of chunks produced by the algorithm to divide the file.
Each chunk is reserved to ensure that 2 process to not work on the same part of the file, but if a race condition allowed this to happen (it's possible)
then the only consequence would be that the same calcualtion of word count digest is written twice.


MongoDB
I used a dockerised version of mongo


## Improvements
If I had more time to do the exercise, I would:
1: Use MongoDB sharding and map/reduce function instead of selecting and merging on the java side.
2: I only had time for tests that were essential for rudimentary tdd.  Of course integration tests should be added and a better coverage of unit tests.

I found the torrent an easier way to get the dump than the direct download:
https://itorrents.org/torrent/B4531B9C5337B0B078CE8D8EEC3F8E83B2AEA583.torrent

## Assumptions:
1: It is cheaper to read an artitrary chunk of data using a SeekableByteChannel and throw away the part at the end that is not a complete mediawiki than it is to serially parse the whole file.
2: There is a lot of content in the mediawiki xml dump.  I am only analysing text content covering <comments>, <text> and <title> everything else is ignored.
3: There is no point xml-parsing the file, this would affect performance the xml compliance to the schema is irrelevant.


