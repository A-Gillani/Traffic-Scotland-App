# Traffic app testing

Run `gradlew.bat :app:assembleDebug :app:testDebugUnitTest` from the project root on Windows. On macOS or Linux use `./gradlew :app:assembleDebug :app:testDebugUnitTest`. The first run downloads build dependencies and the Robolectric Android runtime. Gradle writes the test report to `app/build/reports/tests/testDebugUnitTest/index.html`.

The debug build succeeded. All 12 traffic-specific tests passed, alongside the original example test. The data tests use small, hand-written XML fixtures. The UI tests run the production screens on Robolectric API 35 with three sample reports and a controlled feed provider. They require no Traffic Scotland or Google credentials.

| Check | Expected | Observed in automated run |
| --- | --- | --- |
| Two XML situations and lane counts | Keep the road, comment and both lane counts on their own record | Passed. The M8 report retained one restricted lane and two operational lanes. |
| Location formats | Read both endpoints of a segment and an independent point | Passed. The point and segment values matched the fixtures. |
| Bad coordinates | Keep the report available in the list and continue parsing | Passed. The invalid coordinates were discarded and the following record was read. |
| Roadworks duration | Exactly 24 hours is short. Any longer is long. An invalid interval has no duration label. | Passed, including a one-second boundary case and a time-zone offset. |
| Road and category matching | Trim the search, ignore case, distinguish M8 from M80 and respect category | Passed. |
| Search, details, back and empty results | Searching M8 finds two records. Roadworks shows one. Back returns to it. M90 has an empty result message. | Passed. The selected details contained the expected lane-repair description. |
| Activity recreation and landscape | Retain the road and selected tab on recreation. Landscape places controls beside results. | Both tests passed. |
| Map entry | Opening the map from an M8 Roadworks list keeps the road and category | Passed. The map filter showed Roadworks Only and the road label showed M8. |
| Failed refresh | Retain records from the last successful request and show a retry message | Passed. Two M8 records remained visible after the simulated failure. |
| Successful empty feed | Clear old records from that category only | Passed. An empty roadworks result retained both incidents. |
| Failed first request | Show how to retry and distinguish the failure from a successful empty feed | Passed. The incidents screen showed a refresh prompt. |

## Device checks still to record

Use the normal app with your configured credentials. For each check, record the actual outcome and capture the relevant screen and source evidence.

| Check | Expected | Actual result |
| --- | --- | --- |
| Compare a current incident and roadworks record against a timestamped Traffic Scotland XML capture | The road, public comment, available lane counts, dates and position agree with the source | Pending device check |
| Filter to a road with multiple reports and open Map | Only matching reports with coordinates have pins. Tapping a pin shows its road and comment. The distance control narrows the results around Glasgow. | Pending device check |
| Rotate on a filtered list, open details and return | The search and category are retained and the list remains usable in both orientations | Pending device check |
| Refresh, switch to airplane mode, and refresh again. Also launch offline after stopping the app. | The running app retains the previous list and indicates stale data. A fresh offline launch shows a retry message. | Pending device check |
| Leave the app visible through its five-minute refresh interval | Both feeds refresh. The active road filter remains applied. | Pending device check |

## Local setup and icons

Android Studio uses the local SDK path. Live data requires the existing `TRAFFIC_CLIENT_ID`, `TRAFFIC_CLIENT_KEY` and `MAPS_API_KEY` entries in the project's untracked `local.properties`. Keep their values out of test fixtures and source control.

The traffic pictograms and navigation icons are original Android vector paths in `app/src/main/res/drawable/traffic_*.xml`, drawn for this revision. The list retains a written situation type alongside each icon.
