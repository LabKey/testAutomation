<?xml version="1.0"?>
<!--********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
 * Chicago, IL 60661 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************-->
 <xsl:stylesheet
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.w3.org/TR/html4/strict.dtd" >

    <xsl:output method="html"/>

    <xsl:variable name="messagelist" select="/cruisecontrol/build//target/task/message"/>
    <xsl:variable name="messageCount" select="count(/cruisecontrol/build//target/task/message)" />
    <!-- If 'lineCount' hasn't been passed in, we'll define it here to include all messages. -->
    <xsl:param name="lineCount" />

    <xsl:variable name="lineCountValue">
	    <xsl:choose>
		    <xsl:when test="number($lineCount) &gt; 0">
			    <xsl:value-of select="number($lineCount)" />
		    </xsl:when>
		    <xsl:otherwise>
			    <xsl:value-of select="$messageCount" />
		    </xsl:otherwise>
	    </xsl:choose>
    </xsl:variable>
    <xsl:variable name="startIndex" select="$messageCount - $lineCountValue" />


    <xsl:template match="/" mode="errors">
            <table align="center" cellpadding="0" cellspacing="1" border="0" width="95%">
                <tr>
                    <td class="compile-sectionheader">
                        &#160;Build output:
                    </td>
                </tr>
                <tr>
                    <td>
                        <span class="compile-data">
                            <xsl:if test="$lineCountValue &lt; $messageCount">
				<h4>Build output truncated.  Only displaying last <xsl:value-of select="$lineCount" /> messages.</h4>
			    </xsl:if>
                        <xsl:for-each select="$messagelist">
                            <xsl:if test="position() &gt; $startIndex">
				    <xsl:variable name="message" select="text()"/>
				    <xsl:variable name="messageLength" select="string-length($message)"/>
				    <xsl:if test="$messageLength &gt; 150">
					<xsl:variable name="breakableMessage" select="translate($message, ';', ' ')" />
					<p style="margin: 0px; padding: 0px;"><xsl:value-of select="$breakableMessage" /></p>
				    </xsl:if>
				    <xsl:if test="$messageLength &lt;= 150">
					<p style="margin: 0px; padding: 0px;"><xsl:apply-templates select="$message"/></p>
				    </xsl:if>
			    </xsl:if>
                        </xsl:for-each>
                        </span>
                    </td>
                </tr>
            </table>
    </xsl:template>
</xsl:stylesheet>
