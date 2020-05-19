# dotnet-sdk-plugin

This is a plugin for [Jenkins](https://www.jenkins.io), providing convenient use of
[.NET SDKs](https://dotnet.microsoft.com/download/dotnet-core), specifically the SDKs for .NET Core and .NET 5.0.

The first release will include:
- configuration of named SDKs as global tools, with automatic installation via download
  - `dotnetsdk` in the `tools` section of a declarative pipeline
- a build wrapper, to set up the environment for a particular .NET SDK
  - a "With .NET" section in freestyle jobs
  - `withDotNet` in pipelines
- several builders, for [common `dotnet` commands](https://docs.microsoft.com/en-us/dotnet/core/tools/):
  - `dotnetBuild`: runs "`dotnet build`"
  - `dotnetClean`: runs "`dotnet clean`"
  - `dotnetListPackage`: runs "`dotnet list package`"
  - `dotnetPack`: runs "`dotnet pack`"
  - `dotnetRestore`: runs "`dotnet restore`"
  - `dotnetTest`: runs "`dotnet test`"

The builders are just for convenience; when using the build wrapper, any dotnet command line can
be executed using a batch or shell step, as applicable.


## Release Notes

No releases yet.
