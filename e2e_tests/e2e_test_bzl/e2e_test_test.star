load(":e2e_test.bzl", "adapter_e2e_test", "adapter_e2e_test_workflow")

# Command to update the golden (go/bzl_golden_test#bzl-golden-test-suite):
# $ blaze test \
#   --nocache_test_results \
#   --test_strategy=local \
#   --test_arg=--update //third_party/java_src/gma_sdk_mediation/e2e_tests/e2e_test_bzl:e2e_test_bzl_test

adapter_e2e_test(
    name = "my_e2e_test",
    test_class = "MyTestClass",
    args = ["--my_arg"],
    data = ["//my/data:dep"],
    tags = ["my_tag"],
    device_fixture_scripts = ["my_script.sh"],
    timeout = "eternal",
)

adapter_e2e_test_workflow(
    name = "my_workflow",
    targets = [":my_e2e_test"],
)

expected_output = r"""
shared_lab_android_device(
    name = "my_e2e_test_device",
    models = ["pixel 8a"],
    versions = ["34"],
)

extra_instrumentation_config(
    name = "my_e2e_test_extra_instrumentation_config",
    prefix_android_test = True,
)

android_logcat_decorator(
    name = "my_e2e_test_logcat",
    log_filter_specs = ["*:Verbose"],
    log_to_file = True,
)

android_hd_video_decorator(name = "my_e2e_test_video")

android_dump_sys_decorator(
    name = "my_e2e_test_dumpsys",
    dumpsys_type = "all",
    log_to_file = True,
)

android_screenshot_decorator(name = "my_e2e_test_screenshot")

android_device_script_fixture(
    name = "my_e2e_test_fixture_0",
    script = "my_script.sh",
)

android_instrumentation_test(
    name = "my_e2e_test",
    timeout = "eternal",
    args = [
        "--test_filter_spec=TEST_NAME",
        "--test_method_full_names=com.google.ads.mediation.testapp.MyTestClass",
        "--my_arg",
    ],
    data = ["//my/data:dep"],
    fixtures = [":my_e2e_test_fixture_0"],
    shard_count = 1,
    support_apps = ["//third_party/java_src/gma_sdk_mediation/e2e_tests/e2e_test_bzl:target_apk"],
    tags = ["my_tag"],
    target_device = ":my_e2e_test_device",
    test_app = "//third_party/java_src/gma_sdk_mediation/e2e_tests/e2e_test_bzl:test_apk",
    utp_host_plugins = [
        ":my_e2e_test_extra_instrumentation_config",
        ":my_e2e_test_logcat",
        ":my_e2e_test_video",
        ":my_e2e_test_dumpsys",
        ":my_e2e_test_screenshot",
    ],
)

guitar_workflow_test(
    name = "my_workflow",
    integration_test = guitar.IntegrationTest(
        extra_env_params = {
            "TARGET_APK_PATH": "",
            "TEST_APK_PATH": "",
        },
        tests = [guitar.Tests(
            args = [
                "--test_output=streamed",
                "--notest_loasd",
                "--nocache_test_results",
            ],
            blaze_flags = [
                "--define=TARGET_APK_PATH={TARGET_APK_PATH}",
                "--define=TEST_APK_PATH={TEST_APK_PATH}",
            ],
            execution_method = "LOCAL",
            flaky_test_attempts = 10,
            targets = [":my_e2e_test"],
        )],
    ),
)
"""
