/*
 * MIT License
 *
 * Copyright (c) 2026 Jason Pearson
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
package dev.jasonpearson.android.data.photography

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GalleryParserTest {

    @Test
    fun `responsive and image card photos retain order and select expected urls`() {
        val html =
            """
            <figure class="kg-gallery-card">
              <div class="kg-gallery-image">
                <img src="https://example.com/PXL_20240916_143210123.jpg" width="4032" height="3024"
                  srcset="https://example.com/size/w500/PXL_20240916_143210123.jpg 500w,
                          https://example.com/size/w600/PXL_20240916_143210123.jpg 600w,
                          https://example.com/size/w1000/PXL_20240916_143210123.jpg 1000w,
                          https://example.com/size/w2400/PXL_20240916_143210123.jpg 2400w,
                          https://example.com/size/w3000/PXL_20240916_143210123.jpg 3000w" />
              </div>
            </figure>
            <figure class="kg-gallery-card">
              <div class="kg-gallery-image">
                <img src="https://example.com/IMG_20240208_subject.jpg" width="1920" height="1080"
                  srcset="https://example.com/size/w550/IMG_20240208_subject.jpg 550w,
                          https://example.com/size/w800/IMG_20240208_subject.jpg 800w,
                          https://example.com/size/w1600/IMG_20240208_subject.jpg 1600w" />
              </div>
            </figure>
            <figure class="kg-image-card">
              <img src="https://example.com/portrait.jpg" width="1200" height="1800" />
            </figure>
            <figure class="kg-image-card">
              <img src="https://example.com/PXL_20240916_143210123.jpg" />
            </figure>
            """
                .trimIndent()

        val photos = parseGallery(html)

        assertEquals(3, photos.size)
        assertEquals(
            listOf(
                "https://example.com/size/w2400/PXL_20240916_143210123.jpg",
                "https://example.com/size/w1600/IMG_20240208_subject.jpg",
                "https://example.com/portrait.jpg",
            ),
            photos.map { it.fullUrl },
        )
        assertEquals("https://example.com/size/w600/PXL_20240916_143210123.jpg", photos[0].thumbUrl)
        assertEquals("https://example.com/size/w2400/PXL_20240916_143210123.jpg", photos[0].fullUrl)
        assertEquals(4032, photos[0].width)
        assertEquals(3024, photos[0].height)
        assertEquals("2024-09-16", photos[0].takenOn)

        assertEquals("https://example.com/size/w550/IMG_20240208_subject.jpg", photos[1].thumbUrl)
        assertEquals("https://example.com/size/w1600/IMG_20240208_subject.jpg", photos[1].fullUrl)
        assertEquals(1920, photos[1].width)
        assertEquals(1080, photos[1].height)
        assertEquals("2024-02-08", photos[1].takenOn)

        assertEquals("https://example.com/portrait.jpg", photos[2].thumbUrl)
        assertEquals("https://example.com/portrait.jpg", photos[2].fullUrl)
        assertEquals(1200, photos[2].width)
        assertEquals(1800, photos[2].height)
        assertNull(photos[2].takenOn)
        assertNull(photos[2].caption)
        assertNull(photos[2].altText)
    }

    @Test
    fun `captions come from the card and alt text from the image`() {
        val html =
            """
            <figure class="kg-card kg-gallery-card kg-card-hascaption">
              <div class="kg-gallery-container"><div class="kg-gallery-row">
                <div class="kg-gallery-image"><img src="https://example.com/a.jpg" alt="Harbor at dusk"></div>
                <div class="kg-gallery-image"><img src="https://example.com/b.jpg" alt=" "></div>
              </div></div>
              <figcaption><span style="white-space: pre-wrap;">Trip to </span><b>Maine</b></figcaption>
            </figure>
            <figure class="kg-card kg-image-card kg-card-hascaption">
              <img src="https://example.com/c.jpg" alt="">
              <figcaption>   </figcaption>
            </figure>
            <figure class="kg-card kg-image-card">
              <a href="https://example.com/c-large.jpg"><img src="https://example.com/d.jpg" alt="Linked"></a>
              <figcaption>Single shot</figcaption>
            </figure>
            """
                .trimIndent()

        val photos = parseGallery(html)

        assertEquals(
            listOf("Trip to Maine", "Trip to Maine", null, "Single shot"),
            photos.map { it.caption },
        )
        assertEquals(listOf("Harbor at dusk", null, null, "Linked"), photos.map { it.altText })
    }

    @Test
    fun `images without a usable source are skipped`() {
        val html =
            """
            <figure class="kg-card kg-image-card"><img alt="no src"></figure>
            <figure class="kg-card kg-image-card"><img src="" alt="blank"></figure>
            <figure class="kg-card kg-image-card"><img src="/content/images/relative.jpg"></figure>
            <figure class="kg-card kg-image-card">
              <img src="https://example.com/ok.jpg" width="wide" height="" srcset="not a srcset, https://example.com/x.jpg 0w">
            </figure>
            <p><img src="https://example.com/not-a-card.jpg"></p>
            """
                .trimIndent()

        val photos = parseGallery(html)

        assertEquals(1, photos.size)
        assertEquals("https://example.com/ok.jpg", photos[0].thumbUrl)
        assertEquals("https://example.com/ok.jpg", photos[0].fullUrl)
        assertNull(photos[0].width)
        assertNull(photos[0].height)
    }

    @Test
    fun `empty page has no photos`() {
        assertEquals(emptyList<GalleryPhoto>(), parseGallery(""))
    }
}
