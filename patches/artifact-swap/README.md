# Artifact Swap CLI patches

The CLI in use is a patched build of [block/artifact-swap](https://github.com/block/artifact-swap)
v0.1.12, published as a release asset on this repo
(`cli-<version>` tags) and downloaded by `scripts/artifact-swap/install-cli.sh`.

`artifactswap-cli-0.1.12-kaeawc.1.patch` — two GitHub Packages compatibility fixes,
both candidates for upstream contribution:

1. **NetworkModule**: upstream skips the Authorization header on GET/HEAD (valid for
   Artifactory instances with anonymous reads); GitHub Packages requires authentication
   on every request, so the header (Basic, `token:<token>`) is now always attached.
   Empirically verified: unauthenticated reads 401; Basic/Bearer/token schemes all 200.
2. **maven `Metadata` model**: `<release>` made nullable — GitHub Packages' generated
   `maven-metadata.xml` omits the element that Artifactory includes, which made every
   metadata parse fail (`KotlinInvalidNullException`).

To rebuild: clone upstream at `v0.1.12`, apply the patch, set `version` in
`gradle.properties`, run `./gradlew :cli:distZip`, and attach
`cli/build/distributions/artifactswap-<version>.zip` to a `cli-<version>` release.
