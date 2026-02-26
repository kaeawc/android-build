/*
 * MIT License
 *
 * Copyright (c) 2024 Jason Pearson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.jasonpearson.android.resume

// Resume data
val resumeItems =
    listOf(
        ResumeItem.Profile(
            "Over a decade of experience in Mobile, Backend, DevOps, BI, and now AI. I love building and maintaining software that helps other people."
        ),
        ResumeItem.Experience(
            title = "Staff Software Engineer",
            company = "webAI",
            location = "Remote",
            period = "March 2024 — Present",
            responsibilities =
                listOf(
                    "Created strategic pillars and productivity app to fulfill the business vision of wanting to create AI tools consumable by anyone.",
                    "Built a MacOS desktop app with SwiftUI integrated in a KMP codebase that is capable of multiple conversations with orchestrated on-device LLMs leveraging webAI's platform.",
                    "Started dogfooding practices with internal employees, secured data at rest with GRDB+SQLCipher.",
                    "Designed and implemented the distributed systems to support on-demand networked RAG that opened up distributed systems for the business.",
                    "Revamped the entire engineering org's documentation by working with individuals on pain points during onboarding and executives seeking the availability of information.",
                    "Created vision and strategy for a KMP on-device ML Mobile SDK that would tie together with existing ecosystem of products via PyTorch ExecuTorch & MLX.",
                ),
        ),
        ResumeItem.Experience(
            title = "Senior Staff Android Engineer",
            company = "Hinge",
            location = "New York",
            period = "November 2016 — October 2023",
            responsibilities =
                listOf(
                    "Single-handedly developed Hinge's Android app from the ground up, leveraging Kotlin.",
                    "Onboarded and mentored 26 Android contributors, cultivating a collaborative guild culture rooted in trust, kindness, and mentorship.",
                    "Planned the execution of half a dozen features while contributing my own to a new multi-tier monetization initiative. This initiative led to a 30% increase in ARR.",
                    "Spearheaded and aligned Hinge's internationalization efforts with C-suite to adapt all department workflows.",
                    "Orchestrated multiple transformative updates to the codebase over a decade to ensure our technology stack remained ahead of the curve.",
                    "R&D efforts established an industry-leading CI/CD pipeline that optimized for speed and quality.",
                ),
        ),
        ResumeItem.Experience(
            title = "Software Engineer",
            company = "Hinge",
            location = "New York",
            period = "March 2014 — November 2016",
            responsibilities =
                listOf(
                    "Pioneered the system design that Hinge still uses in production today.",
                    "Created internal tools to aid QA and customer service.",
                    "Introduced testing, CI/CD, and modern monitoring & alerting systems, async data processing pipelines.",
                    "During Hinge's 2016 pivot I implemented load testing to guarantee our relaunch would be successful.",
                    "Designed and tested Hinge's original payment processing systems.",
                ),
        ),
        ResumeItem.Experience(
            title = "Software Engineer",
            company = "Echo360, Inc.",
            location = "New York",
            period = "October 2012 — October 2013",
            responsibilities =
                listOf(
                    "Built real-time collaborative tools that got startup ThinkBinder acquired by Echo360.",
                    "Handled product and platform workload for a greenfield project while mentoring new team members.",
                ),
        ),
        ResumeItem.Experience(
            title = "IT Tools Developer",
            company = "Shutterstock",
            location = "New York",
            period = "May 2011 — January 2012",
            responsibilities =
                listOf(
                    "Built comprehensive BI dashboards for several business units and executives.",
                    "Integrated cube reader for drill-through functionality on the data warehouse.",
                ),
        ),
        ResumeItem.Skills(
            skills =
                listOf(
                    "Android",
                    "Kotlin",
                    "Compose & XML UX",
                    "Animation",
                    "KMP",
                    "Gradle",
                    "Continuous Integration",
                    "Leadership",
                    "Communication",
                    "Mentorship",
                    "System Design",
                    "Project Management",
                )
        ),
        ResumeItem.Education(
            degree = "Bachelor of Science",
            institution = "New Jersey Institute of Technology",
            period = "January 2005 — January 2010",
        ),
        ResumeItem.Talks(
            talks =
                listOf(
                    ResumeItem.Talk(
                        title = "From Laptop Builds to Advanced CI",
                        event = "Droidcon London",
                        date = "October 2023",
                    ),
                    ResumeItem.Talk(
                        title = "MotionLayout & RecyclerView",
                        event = "Droidcon Italy",
                        date = "November 2020",
                    ),
                    ResumeItem.Talk(
                        title = "Advanced MotionLayout",
                        event = "Droidcon SF",
                        date = "November 2019",
                    ),
                )
        ),
    )
