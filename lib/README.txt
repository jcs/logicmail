To support the build process, this directory needs several files from
external sources.  These files are laid out as follow:

  Path                   | Description
-------------------------|--------------------------------------------
 jde/                    | Files from the RIM BlackBerry JDK
     bin/                |
         rapc.jar        | RAPC utility
         preverify.exe   | From the JDE, J2ME SDK, or equivalent
     lib/                |
         net_rim_api.jar | BlackBerry Java API
 xmltask.jar             | XMLTask
 bb-ant-tools.jar        | BlackBerry Ant Tools (trunk, r62, built with patch)
 bb-ant-tools.patch      | Patch to BlackBerry Ant Tools
 ant-contrib-1.0b3.jar   | Ant-Contrib Tasks
 hammockmaker-2.1.0.jar  | HammockMaker tool (from the Hammock distribution)
 AnalyticsServicev0.9Beta.jar     | BlackBerry Analytics Service SDK
 AnalyticsServicev0.9Beta-doc.zip | JavaDocs for above
 webtrends-template.xml           | Analtics configuration template


Sources:
--------
RIM BlackBerry JDK
    http://www.blackberry.com/
J2ME SDK
    http://java.sun.com/
XMLTask
    http://www.oopsconsultancy.com/software/xmltask/
BlackBerry Ant Tools
    http://bb-ant-tools.sourceforge.net/
Ant-Contrib Tasks
    http://ant-contrib.sourceforge.net/
Hammock
    http://hammingweight.com/modules/hammock/
BlackBerry Analytics Service SDK
    http://us.blackberry.com/developers/platform/analyticsservice/
    Not included due possible legal issues with redistribution.
    LogicMail can be successfully built and run without these libraries.