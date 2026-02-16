# Policy And Declarations Workbook

Use this workbook to pre-answer Play Console policy sections.

Date baseline: February 16, 2026.

## Current app behavior snapshot

Based on current code and manifest:

- Package: `com.marconius.ohcraps`
- Runtime permissions requested: none
- Account system: none
- Login required: no
- Ads SDK detected: none
- Analytics SDK detected: none
- User content: strategies can be created and stored locally on device
- External sharing: user-initiated Android share sheet for plain text strategy content
- Network behavior: link opens and user-driven external share/email flows

## Privacy policy

- [ ] Keep your existing hosted privacy policy URL ready
- [ ] Confirm it is publicly accessible without login
- [ ] Confirm it states:
- [ ] what data the app collects
- [ ] what data is shared
- [ ] whether data is stored locally only
- [ ] contact email for privacy requests
- [ ] Confirm policy text matches current Android app behavior

## Ads declaration

Suggested answer if unchanged:

- Ads in app: `No`

Only change to `Yes` if you add ad SDKs, ad placements, or ad services.

## App access

Suggested answer if unchanged:

- All functionality available without login or credentials
- App access instructions: not required

## Content rating

- [ ] Complete questionnaire honestly for gambling-related educational content
- [ ] Keep answers consistent with app purpose (strategy/reference content, not real-money wagering transactions in app)
- [ ] Review resulting rating and region availability impact

## Target audience and content

- [ ] Choose intended age groups
- [ ] Confirm app is not primarily directed to children
- [ ] Ensure listing language matches selected audience

## News apps declaration

Suggested answer if unchanged:

- News app: `No`

## Data safety

Suggested baseline if unchanged:

- Data collected by app: `No` (if truly no app-side collection/transmission)
- Data shared by app: `No` (user-driven share sheet can be considered user action outside your backend; verify in current policy UI wording)
- Security practices: confirm encryption in transit if any network collection is ever added

Important:

- Re-check every Data safety answer at release time in case Google form wording changes.
- If you later add crash reporting, analytics, sign-in, cloud sync, or ads, update this section immediately before next release.

## Policy source links

- App setup and release basics: https://support.google.com/googleplay/android-developer/answer/9859152
- Prepare app for review and publication: https://support.google.com/googleplay/android-developer/answer/9859455
- Data safety form overview: https://support.google.com/googleplay/android-developer/answer/10787469
- User Data policy: https://support.google.com/googleplay/android-developer/answer/10144311
