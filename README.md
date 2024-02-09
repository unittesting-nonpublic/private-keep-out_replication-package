# Replication Package of Private - Keep Out? 


## Developer Study
1. Questions and Information Sheet (first page) is included in the [Developer Questionnaire.pdf](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/blob/main/Developer%20Questionnaire.pdf)
2. The raw responses from the developers are included [here](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/blob/main/Developer%20Questionnaire%20Responses.tsv)
3. Finally, the thematic analysis is included [here](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/blob/main/Developer%20Questionnaire%20Free-Text%20(Thematic%20Analysis).xlsx)

## StackOverflow Analysis
[Stackoverflow Threads Thematic Analysis XLSX sheet](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/blob/main/Stackoverflow%20Threads%20(Thematic%20Analysis).xlsx)
- The Stackoverflow API URLs (first tab) used to gather all relevant URLs are in the second tab
- The thematic analysis of both 'Debate Questions' (motivations), 'Debate Answers' (opinions), and 'Practice' are in the same XLSX sheet but in separate tabs.

## Open-Source Study
The result of the Java open-source study is included in the [java-project-stats](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/tree/main/java-projects-stats) folder
and it contains two main parts:
1. The production code [method(s)/field(s)/contructor(s) and it's access modifier(s)](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/blob/main/java-projects-stats/reports-access-modifiers-type.zip) (different parameters and/or access modifiers are considered as different types)
2. [Method(s) and it's access modifier(s)](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/blob/main/java-projects-stats/final_filter_invoked_methods.tsv.zip) being invoked in the test from the production code
   - This includes where it's being invoked (if it's using any test helper and if it's part of the getters and setters)
   - Number of times being invoked. (being GROUPED BY # of invocation)
   
The extra details of the type libraries used to invoke private methods are in this [TSV file](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/blob/main/java-projects-stats/invoked_grouped_Private_only.tsv)

To use the instrumentation:
1. Record all the name methods CUT (this is done via using the [fetch-classes](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/tree/main/java-projects-setup/fetch-classes) framework.
2. Execute the pom-modify [script](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/blob/main/java-projects-setup/pom-modify/modify-project.sh) command on the root of the target project to include the maven-surefire plugin so that when executing the test (`mvn test`), the JUnit Surefire Report will include the loggging statements.
3. Finally, execute the test (from the root mvn folder that contains the parent form) with the [java agent listener](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/tree/main/java-projects-setup/javaagent-listener) that acts as a listener (`mvn test -javaagent:javaagent-1.0-SNAPSHOT-jar-with-dependencies.jar`) that includes the name of all the production code methods involved.
4. The report of the JUnit Surefire report will include the statements from the executed test.
5. To parse the log, use the JUnit-XML parser [script](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/blob/main/java-projects-stats/get_invoked_methods.ipynb) and it'll then remove invocation from internal method/constructor calls of the method.
6. Finally, use the filtering [script](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/blob/main/java-projects-stats/filter_methods.ipynb) that will analyze the test code statically to make (sanity check) confirmation that the methods invoked exist in the test code.

Stats:
Successful Java projects - [Link](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/blob/main/java-projects-stats/java_projects.csv)



