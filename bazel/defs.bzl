"""Module macros: the Bazel counterpart of build-logic's convention plugins.

Every module keeps the Gradle source layout (src/main/kotlin, src/test/kotlin, src/main/res,
src/main/AndroidManifest.xml), so Android Studio's Gradle sync and Bazel read the same files.

    kt_jvm_module           androidbuild.kotlin-jvm        pure Kotlin/JVM module
    kt_android_module       androidbuild.android-library   Android library (compose = True for
                            androidbuild.android-compose)  the Compose compiler)
    kt_android_application  app/build.gradle.kts           the APK, its BuildConfig and tests

Each macro defines the module's library as `:<package name>` (so `//core/model` works as a
label), plus `:tests`, a test_suite with one JUnit 4 target per *Test.kt class.
"""

load("@bazel_skylib//rules:write_file.bzl", "write_file")
load("@rules_android//android:rules.bzl", "android_binary")
load("@rules_java//java:defs.bzl", "java_test")
load("@rules_kotlin//kotlin:android.bzl", "kt_android_library")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

_TEST_SRCS = ["src/test/kotlin/**/*.kt", "src/test/java/**/*.kt"]
_TEST_ROOTS = ["src/test/kotlin/", "src/test/java/"]

_PLUGINS = {
    "compose": "//bazel/kotlin:compose",
    "metro": "//bazel/kotlin:metro",
    "serialization": "//bazel/kotlin:serialization",
}

def _plugins(names):
    return [_PLUGINS[name] for name in names]

def _module_name():
    return native.package_name().split("/")[-1]

def _test_class(path):
    for root in _TEST_ROOTS:
        if path.startswith(root):
            return path[len(root):-len(".kt")].replace("/", ".")
    fail("unexpected test source: " + path)

def _unit_tests(library, deps, plugins, extra_deps = [], excludes = []):
    """A test library compiled once, and a JUnit target per *Test.kt class in it."""
    srcs = native.glob(_TEST_SRCS, exclude = excludes, allow_empty = True)
    if not srcs:
        native.test_suite(name = "tests", tests = [])
        return
    kt_jvm_library(
        name = "test_lib",
        testonly = True,
        srcs = srcs,
        resources = native.glob(["src/test/resources/**"], allow_empty = True),
        resource_strip_prefix = native.package_name() + "/src/test/resources",
        associates = [library],
        plugins = plugins,
        deps = deps + extra_deps + [
            "@maven//:junit_junit",
            "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_test",
        ],
    )
    tests = []
    for src in srcs:
        if not src.endswith("Test.kt"):
            continue
        test_class = _test_class(src)
        name = test_class.split(".")[-1]
        java_test(
            name = name,
            size = "small",
            test_class = test_class,
            runtime_deps = [":test_lib"],
        )
        tests.append(":" + name)
    native.test_suite(name = "tests", tests = tests)

def kt_jvm_module(
        name = None,
        deps = [],
        exports = [],
        plugins = [],
        test_deps = [],
        visibility = ["//visibility:public"]):
    """A pure Kotlin/JVM module (androidbuild.kotlin-jvm).

    Args:
      name: the library's name; defaults to the package's directory name.
      deps: implementation dependencies.
      exports: api dependencies (also compiled against).
      plugins: compiler plugins by name: "metro", "serialization".
      test_deps: extra testImplementation dependencies.
      visibility: visibility of the library.
    """
    name = name or _module_name()
    kt_jvm_library(
        name = name,
        srcs = native.glob(["src/main/kotlin/**/*.kt", "src/main/java/**/*.kt"], allow_empty = True),
        resources = native.glob(["src/main/resources/**"], allow_empty = True),
        resource_strip_prefix = native.package_name() + "/src/main/resources",
        plugins = _plugins(plugins),
        exports = exports,
        deps = deps + exports,
        visibility = visibility,
    )
    _unit_tests(":" + name, deps + exports + test_deps, _plugins(plugins))

def kt_android_module(
        custom_package,
        name = None,
        deps = [],
        exports = [],
        plugins = [],
        compose = False,
        test_deps = [],
        visibility = ["//visibility:public"]):
    """An Android library module (androidbuild.android-library / android-compose).

    Args:
      custom_package: the module's namespace (its R class package).
      name: the library's name; defaults to the package's directory name.
      deps: implementation dependencies.
      exports: api dependencies (also compiled against).
      plugins: compiler plugins by name: "metro", "serialization".
      compose: whether the module uses Jetpack Compose (applies the Compose compiler).
      test_deps: extra testImplementation dependencies.
      visibility: visibility of the library.
    """
    name = name or _module_name()
    plugin_names = plugins + (["compose"] if compose else [])
    resources = native.glob(["src/main/res/**"], allow_empty = True)
    manifest = native.glob(["src/main/AndroidManifest.xml"], allow_empty = True)
    if resources and not manifest:
        native.genrule(
            name = "manifest",
            outs = ["AndroidManifest.xml"],
            cmd = "echo '<manifest package=\"%s\" />' > $@" % custom_package,
        )
        manifest = [":manifest"]
    kt_android_library(
        name = name,
        srcs = native.glob(["src/main/kotlin/**/*.kt", "src/main/java/**/*.kt"], allow_empty = True),
        custom_package = custom_package,
        manifest = manifest[0] if manifest else None,
        # Merge the module's manifest entries (e.g. the widget receivers) into the APK's.
        exports_manifest = 1 if manifest else 0,
        resource_files = resources,
        plugins = _plugins(plugin_names),
        exports = exports,
        deps = deps + exports,
        visibility = visibility,
    )

    # Like Gradle's Android unit tests: plain JVM tests that compile against android.jar.
    _unit_tests(
        ":" + name,
        deps + exports + test_deps,
        _plugins(plugin_names),
        extra_deps = ["//bazel/android:android_jar_for_tests"],
    )

def _build_config(name, package, fields):
    """Generates <package>.BuildConfig as a Kotlin object; DEBUG follows --compilation_mode."""

    def lines(debug):
        return ["package " + package, "", "object BuildConfig {"] + [
            "    const val DEBUG: Boolean = " + debug,
        ] + [
            '    const val %s: String = "%s"' % (field, value)
            for field, value in sorted(fields.items())
        ] + ["}", ""]

    write_file(
        name = name,
        out = "BuildConfig.kt",
        content = select({
            "//bazel/config:release": lines("false"),
            "//conditions:default": lines("true"),
        }),
    )

def kt_android_application(
        custom_package,
        application_id,
        version_code,
        version_name,
        min_sdk,
        target_sdk,
        name = "app",
        build_config = {},
        deps = [],
        plugins = [],
        test_deps = [],
        test_excludes = [],
        android_test_deps = [],
        debug_key = None,
        proguard_specs = []):
    """The application module (the app's android_binary, a library for its code, and tests).

    Defines :lib (the app's code and resources), :<name> (the APK), :tests (unit tests) and,
    when src/androidTest exists, :instrumentation_tests_apk (the instrumentation APK).

    Args:
      custom_package: the app's namespace.
      name: the APK target's name.
      application_id: the applicationId.
      version_code: versionCode.
      version_name: versionName (also BuildConfig.VERSION_NAME).
      min_sdk: minSdk.
      target_sdk: targetSdk.
      build_config: extra BuildConfig string fields.
      deps: implementation dependencies.
      plugins: compiler plugins by name ("compose" is always applied).
      test_deps: extra unit test dependencies.
      test_excludes: unit test sources to leave out (globs).
      android_test_deps: instrumentation test dependencies.
      debug_key: keystore that signs debug builds.
      proguard_specs: R8 rules for optimized (-c opt) builds.
    """
    _build_config(
        name = "build_config",
        package = custom_package,
        fields = dict(build_config, VERSION_NAME = version_name),
    )
    plugin_targets = _plugins(plugins + ["compose"])

    # The app's code and resources. Its manifest only declares minSdk (so aapt2 accepts
    # resources like adaptive icons); src/main/AndroidManifest.xml is the APK's manifest.
    write_file(
        name = "lib_manifest",
        out = "lib/AndroidManifest.xml",
        content = [
            '<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="%s">' % custom_package,
            '    <uses-sdk android:minSdkVersion="%d" />' % min_sdk,
            "</manifest>",
        ],
    )
    kt_android_library(
        name = "lib",
        srcs = native.glob(["src/main/kotlin/**/*.kt", "src/main/java/**/*.kt"], allow_empty = True) +
               [":build_config"],
        custom_package = custom_package,
        manifest = ":lib_manifest",
        resource_files = native.glob(["src/main/res/**"]),
        plugins = plugin_targets,
        deps = deps,
        visibility = ["//visibility:private"],
    )
    manifest_values = {
        "applicationId": application_id,
        "minSdkVersion": str(min_sdk),
        "targetSdkVersion": str(target_sdk),
        "versionCode": str(version_code),
        "versionName": version_name,
    }
    android_binary(
        name = name,
        custom_package = custom_package,
        manifest = "src/main/AndroidManifest.xml",
        manifest_values = manifest_values,
        debug_key = debug_key,
        proguard_specs = select({
            "//bazel/config:release": proguard_specs,
            "//conditions:default": [],
        }),
        shrink_resources = select({
            "//bazel/config:release": 1,
            "//conditions:default": 0,
        }),
        deps = [":lib"],
    )
    _unit_tests(
        ":lib",
        deps + test_deps,
        plugin_targets,
        extra_deps = ["//bazel/android:android_jar_for_tests"],
        excludes = test_excludes,
    )

    android_test_srcs = native.glob(["src/androidTest/kotlin/**/*.kt", "src/androidTest/java/**/*.kt"], allow_empty = True)
    if android_test_srcs:
        native.genrule(
            name = "instrumentation_manifest",
            outs = ["androidTest/AndroidManifest.xml"],
            cmd = """cat > $@ <<'EOF'
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="%s.test">
  <uses-sdk android:minSdkVersion="%d" />
  <instrumentation android:name="%s.TestRunner" android:targetPackage="%s" />
</manifest>
EOF""" % (application_id, min_sdk, custom_package, application_id),
        )
        kt_android_library(
            name = "instrumentation_lib",
            testonly = True,
            srcs = android_test_srcs,
            custom_package = custom_package + ".test",
            manifest = ":instrumentation_manifest",
            plugins = plugin_targets,
            deps = [":lib"] + deps + android_test_deps,
            visibility = ["//visibility:private"],
        )
        android_binary(
            name = "instrumentation_tests_apk",
            testonly = True,
            custom_package = custom_package + ".test",
            instruments = ":" + name,
            manifest = ":instrumentation_manifest",
            manifest_values = dict(manifest_values, applicationId = application_id + ".test"),
            debug_key = debug_key,
            deps = [":instrumentation_lib"],
        )
