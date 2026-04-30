package com.marconius.ohcraps.ui

data class AboutLink(
	val title: String,
	val url: String
)

val aboutIntroParagraphs: List<String> = listOf(
	"Oh Craps! brings together my accessible collection of Craps strategies for quick reference at home or at the table.",
	"The app updates whenever new strategies are added to the main Oh Craps site, and the Create Strategy tab lets you write, save, and submit your own strategies from your phone.",
	"This app is built with a Blind-first accessibility approach, and supports all assistive technologies on Android."
)

val aboutReferenceIntro =
	"These YouTube channels and sites inspired me to learn more about Craps overall and have videos showing most of the strategies in action."

val aboutReferenceLinks: List<AboutLink> = listOf(
	AboutLink("Color Up on YouTube", "https://www.youtube.com/channel/UCPZ2kcfmtAhnf_RVc9fZzNA"),
	AboutLink("Color Up Club", "https://www.colorup.club"),
	AboutLink("Casino Quest on YouTube", "https://www.youtube.com/channel/UCpyLp493L8QjrJ4PaL5NOrg"),
	AboutLink("Casino Quest Website", "https://www.casinoquest.biz"),
	AboutLink("Let It Roll on YouTube", "https://www.youtube.com/channel/UCe5-Y4pWeudzfeAoaHXT6pA"),
	AboutLink("Craps Hawaii on YouTube", "https://www.youtube.com/channel/UCsVgwCV1yVN5MbFmdPBGrEA"),
	AboutLink("Vince Armenti on YouTube", "https://www.youtube.com/channel/UCJx5jilpl2M9dcq0m8tMExQ"),
	AboutLink("Uncle Angelo on YouTube", "https://www.youtube.com/channel/UCe9uuSMPiwHmhthj1ijQLSA"),
	AboutLink("/r/Craps on Reddit", "https://www.reddit.com/r/craps/"),
	AboutLink("Square Pair on YouTube", "https://www.youtube.com/channel/UCXpqqBCl5qOOHOfbHLSZ9og"),
	AboutLink("Oh Craps! Main Website", "https://marconius.com/craps/"),
	AboutLink("Oh Craps! Game on Github", "https://github.com/marconiusiii/OhCraps")
)

val aboutCreditsParagraphs: List<String> = listOf(
	"Created by Marco Salsiccia",
	"Accessibility-First Design and Development",
	"This app is built and maintained independently as one of my personal passion projects. If you've enjoyed using it and would like to support ongoing updates and improvements, you're welcome to leave a tip. There's never any obligation."
)

val aboutCreditsLinks: List<AboutLink> = listOf(
	AboutLink("Tip the Dealer", "https://www.paypal.me/marconius")
)

val aboutResponsibleGamblingParagraphs: List<String> = listOf(
	"Gambling should always be approached as entertainment, not as a way to make money. The strategies presented in this app are educational examples only and do not guarantee winnings or reduce the inherent risk involved in casino games.",
	"If gambling ever stops feeling fun, or if you feel pressure to chase losses, it may be a sign to take a break or seek support. Help is available, and reaching out is a positive step."
)

val aboutResponsibleGamblingLinks: List<AboutLink> = listOf(
	AboutLink("National Problem Gambling Helpline (United States)", "https://www.ncpgambling.org/help-treatment/"),
	AboutLink("Gamblers Anonymous", "https://www.gamblersanonymous.org/"),
	AboutLink("International Gambling Support Resources", "https://www.gamblingtherapy.org/")
)
