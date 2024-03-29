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
   - This includes where it's being invoked (if it's using any test helper and if it's part of the setup and teardown methods)
   - Number of times being invoked. (being GROUPED BY # of invocation)
   
The extra details of the type libraries used to invoke private methods are in this [TSV file](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/blob/main/java-projects-stats/invoked_grouped_Private_only.tsv)

To use the instrumentation:
1. Record all the name methods CUT (this is done via using the [fetch-classes](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/tree/main/java-projects-setup/fetch-classes) framework).
3. Execute the pom-modify [script](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/blob/main/java-projects-setup/pom-modify/modify-project.sh) command on the root of the target project to include the maven-surefire plugin so that when executing the test (`mvn test`), the JUnit Surefire Report will include the loggging statements.
4. Finally, execute the test (from the root mvn folder that contains the parent form) with the [java agent listener](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/tree/main/java-projects-setup/javaagent-listener) that acts as a listener (`mvn test -javaagent:javaagent-1.0-SNAPSHOT-jar-with-dependencies.jar`) that includes the name of all the production code methods involved.
5. The report of the JUnit Surefire report will include the statements from the executed test.
6. To parse the log, use the JUnit-XML parser [script](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/blob/main/java-projects-stats/get_invoked_methods.ipynb) and it'll then remove invocation from internal method/constructor calls of the method.
7. Finally, use the filtering [script](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/blob/main/java-projects-stats/filter_methods.ipynb) that will analyze the test code statically to make (sanity check) confirmation that the methods invoked exist in the test code.

Stats:
- Successful Java projects - [Link](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/blob/main/java-projects-stats/java_projects.csv)
- Notebook Java study analysis - [Link](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/blob/main/java-projects-stats/OpenSourceProjects.ipynb)
- Projects # of Access Modifiers gathered - [Link](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/blob/main/java-projects-stats/cut_access_modifiers_type.csv) (**THIS IS ONLY FOR METHODS FROM THE PROJECTS, IT DOES NOT INCLUDE OTHER TYPES, e.g. CONSTRUCTORS, FIELDS, ETC.**)

[Fetch-classes](https://github.com/unittesting-nonpublic/private-keep-out_replication-package/tree/main/java-projects-setup/fetch-classes) setup:
- Java 8
- `-DabsPath="PATH_TO_ROOT_MVN_PROJECT" org.example.TestVisibilityChecker`
- Environtment Variable: `MAVEN_HOME="PATH_TO_MVN"`
- The output is two TSV files under the root of the __project__ folder:
     1. __project_name__\_all_method_visibility.tsv, and
        - a TSV file that contains all the methods (under __MavenLauncher.SOURCE_TYPE.APP_SOURCE__) and it's access modifiers

