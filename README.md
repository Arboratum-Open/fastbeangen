# FastBeanGen - Fast Random Data Generator

The library is published on bintray [https://bintray.com/arboratum-open/public/fastbeangen]

In Maven:

~~~~
<dependency>
  <groupId>com.arboratum</groupId>
  <artifactId>fastbeangen</artifactId>
  <version>0.1.6</version>
  <type>pom</type>
</dependency>
~~~~

In Gradle:

~~~~
compile 'com.arboratum:fastbeangen:0.1.6'
~~~~


Temporary until it is accepted by bintray to sync it in jcenter, you have to add the repository to resolve the dependencies

In Maven:
 
~~~~
<?xml version="1.0" encoding="UTF-8" ?>
<settings xsi:schemaLocation='http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd'
          xmlns='http://maven.apache.org/SETTINGS/1.0.0' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>
    <profiles>
        <profile>
            <repositories>
                <repository>
                    <snapshots>
                        <enabled>false</enabled>
                    </snapshots>
                    <id>bintray-arboratum-open-public</id>
                    <name>bintray</name>
                    <url>http://dl.bintray.com/arboratum-open/public</url>
                </repository>
            </repositories>
            <pluginRepositories>
                <pluginRepository>
                    <snapshots>
                        <enabled>false</enabled>
                    </snapshots>
                    <id>bintray-arboratum-open-public</id>
                    <name>bintray-plugins</name>
                    <url>http://dl.bintray.com/arboratum-open/public</url>
                </pluginRepository>
            </pluginRepositories>
            <id>bintray</id>
        </profile>
    </profiles>
    <activeProfiles>
        <activeProfile>bintray</activeProfile>
    </activeProfiles>
</settings>
~~~~

In Gradle:

~~~~
repositories {
    maven {
        url  "http://dl.bintray.com/arboratum-open/public" 
    }
}
~~~~
    
