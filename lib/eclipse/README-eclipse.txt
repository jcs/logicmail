Unlike the automated build environment, which is Ant-based, the normal
development environment for LogicMail is the BlackBerry Java Plug-in for
Eclipse (on Windows). Unfortunately, there is a bug in the latest version
(1.3.0.201102031007-19) of this plugin which prevents it from successfully
building the LogicMail code.

This bug has been reported to RIM numerous times, and is officially documented
in this ticket:
https://www.blackberry.com/jira/browse/TOOL-250

Since a fix is not expected any time soon, I've created a workaround for the
issue. This workaround is implemented as a wrapper for the RIM "rapc.jar"
utility, that filters erroneous entries out of its command-line arguments.

To install this workaround, perform the following steps in each component-pack
plugin directory:
- Go to the component-pack's bin directory
  (e.g. "C:\eclipse\plugins\net.rim.ejde.componentpack4.6.0_4.6.0.23\components\bin")
- Rename the file "rapc.jar" to "rapc-orig.jar"
- Copy the alternate version of "rapc.jar" from this directory to that location

P.S. The source code inside my RAPC wrapper is also provided, in the file
     called "Compiler.java".
