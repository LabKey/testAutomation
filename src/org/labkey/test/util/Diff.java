/*
 * Copyright (c) 2009-2014 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;

public class Diff
{

    public static String diff(String a, String b)
    {
        return _diff(StringUtils.split(a), StringUtils.split(b));
    }


    static String _diff(String[] x, String[] y)
    {
        StringBuilder sb = new StringBuilder();

        // number of lines of each file
        int M = x.length;
        int N = y.length;

        // opt[i][j] = length of LCS of x[i..M] and y[j..N]
        int[][] opt = new int[M + 1][N + 1];

        // compute length of LCS and all subproblems via dynamic programming
        for (int i = M - 1; i >= 0; i--)
        {
            for (int j = N - 1; j >= 0; j--)
            {
                if (equals(x[i], y[j]))
                    opt[i][j] = opt[i + 1][j + 1] + 1;
                else
                    opt[i][j] = Math.max(opt[i + 1][j], opt[i][j + 1]);
            }
        }

        // recover LCS itself and print out non-matching lines to standard output
        int i, j;
        for (i=0, j=0 ; i < M && j < N ; )
        {
            if (x[i].equals(y[j]))
            {
                sb.append("        ").append(x[i]).append("\n");
                i++;
                j++;
            }
            else if (opt[i + 1][j] >= opt[i][j + 1])
                sb.append("<       ").append(x[i++]).append("\n");
            else
                sb.append(">       ").append(y[j++]).append("\n");
        }

        // dump out remainder of one string if the other is exhausted
        while (i < M || j < N)
        {
            if (i == M)
                sb.append(">       ").append(y[j++]).append("\n");
            else if (j == N)
                sb.append("<       ").append(x[i++]).append("\n");
        }
        return sb.toString();
    }

    static boolean equals(String a, String b)
    {
        return a.hashCode()==b.hashCode() && a.equals(b);
    }
}
