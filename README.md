<p align="center">
  <img src="assets/ss.png" alt="Skid Solutions Banner" width="350" />
  <br><br>
  <a href="https://discord.gg/skidsolutions">
    <img src="https://img.shields.io/badge/Discord-Skid%20Solutions-18181b?style=for-the-badge&logo=discord&logoColor=ffffff&labelColor=09090b" alt="Discord" />
  </a>
  <img src="https://img.shields.io/badge/Version-9.1.15-27272a?style=for-the-badge&labelColor=09090b" alt="Version 9.1.15" />
  <img src="https://img.shields.io/badge/Java-17-3f3f46?style=for-the-badge&logo=openjdk&logoColor=ffffff&labelColor=09090b" alt="Java 17" />
  <img src="https://img.shields.io/badge/Gradle-8.14.3-52525b?style=for-the-badge&logo=gradle&logoColor=ffffff&labelColor=09090b" alt="Gradle 8.14.3" />
</p>

<p align="center">
  <a href="https://discord.gg/skidsolutions">https://discord.gg/skidsolutions</a>
</p>

<p align="center">
  <img src="assets/divider.svg" alt="" width="720" />
</p>

<p align="center">
  <img src="assets/jak.jpg" alt="jak" height="150" />
  <img src="assets/hack.jpg" alt="hack" height="150" />
  <img src="assets/deobf2.png" alt="deobf2" height="150" />
  <img src="assets/deobf.jpg" alt="deobf" height="150" />
</p>

<p align="center">
  <sub>This is not a reconstruction nor a remake.</sub>
</p>

<p align="center">
  <img src="assets/divider.svg" alt="" width="720" />
</p>

## How to build

### Prerequisites
- **Java Development Kit**: JDK 17 (the Gradle wrapper 8.14.3 does not run on newer JDKs)

### Building OpenStringer

Run from this folder:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"

.\gradlew.bat clean build
```

Generated `build/libs/`:
- `OpenStringer-9.1.15.jar`
- `OpenStringer-9.1.15-sources.jar`

### Running

```powershell
java -jar build\libs\OpenStringer-9.1.15.jar [options] <src> <dest>
```

No license file, activation code, or online check is required.

Common options:
- `-configFile <file.xml>` — load XML protection configuration
- `-verbose` — enable verbose logging
- `-optimize` — strip debug info (line numbers, local variables, source file)
- `-checkCaller` — enable caller-context string encryption
- `-integrityProtection` — enable bytecode and JAR integrity checks
- `-resourceEncryption` — enable resource encryption and stream wrappers
- `-target <1.5|1.6|1.7|1.8|9>` — force target bytecode version
- `-info` — display product information
