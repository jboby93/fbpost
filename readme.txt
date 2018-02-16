fbpost v2.0 - a simple connector for facebook functions
author: jboby93

depends on: facebook4j, json-simple
==================================================

building:
=========================
just run the build.sh script; requires java 1.7+

usage:
=========================
./run.sh [args]

pages.lst
=========================
create one of these file to specify shorthand aliases for your pages; each line of the file should be of the form
	[alias]=[page id]

fbpost loads this on each run before processing arguments, so instead of having to remember page ids you can use your own aliases instead!

arguments and actions:
=========================
-auth [authfile]
	specify a graph api authentication file ("authfile") to use for connecting to facebook. with the exception of the -share action, ALL actions arguments must come AFTER an authfile specification.

	to create one of these authfiles, use this format:
		AppID=[your app id]
		AppSecret=[your app secret here]
		AccessToken=[your access token here]

	to have your token extended when the authfile is loaded, add the line "NeedsExtended=true" to the end of the file.

-extend
	extends the access token specified in the loaded authfile

	returns: nothing

-post (--text/--file [text or filename containing the text to post]) (--image [imagefile])
	makes a post

	if --text, the text is specified as an argument: --text "hello world!"
	if --file, the post text comes from a file: --file foo.txt

	if --image is specified then the post will also include the image named [imagefile].

	--text/--file is not required for an image post.

	returns: the id of the created post

-comment --page [page] --postid [post id] (--text/--file [text or filename containing the text to post]) (--image [imagefile])
	comments on the specified post with the given text

	all of the variants of the post action arguments apply here as well, as long as the page and post ids are given.

	so an action string to post an image comment with text from foo.txt would be
	-comment --page mypageid --postid mypostid --image foo.jpg --file foo.txt

	--text/--file is not required for an image comment

	to post a comment reply, substitute the post id that contains the comment as the page id, and the destination comment id as the post id
		ex) -comment --page [post id] --postid [comment id] --text "comment reply!"

	returns: id of the created comment

-share --url [url] (--text/--file [text/file]) --to [page1] --as [authfile1] ... --to [pageN] --as [authfileN]
	shares a link (with optional text or text from file) to one or more pages, given their ids and authfiles

	returns: listing of all created posts

-getpost --page [page] --postid [post id] (--json)
	gets info about a single post, optionally returning a json object instead of printing the info to stdout

-getreacts --page [page] --postid [post id] (--json)
	gets reactions from a post

-getcomments --page [page] --post [post id] (--json)
	gets comments from a post

-postsreport --page [page] --start [mm-dd-yyyy] --end [mm-dd-yyyy] (--html) (--json) (--saveas [file])
	returns a report of all posts from a page within the provided date range

	if --html, the report will be returned as html-formatted text
	if --json, the report will be returned as a json object
	--html and --json cannot be used together!
	if no format is specified, the report is printed to stdout in plaintext

	optionally, use the --saveas switch to save the report to a file

-heartbeat
	just retrieves some posts from the page and does nothing with them; meant to keep an access token from expiring from inactivity


examples:

make a post
> ./run.sh -auth [authfile] -post --text "hello world!"
< 1234567890987654321 [the id of the created post]

get posts on page w/ id 12345 made in december 2017
> ./run.sh -auth [authfile] -postsreport --page 12345 --start 12-1-2017 --end 12-31-2017 --json
< [json listing of posts found in this range]
