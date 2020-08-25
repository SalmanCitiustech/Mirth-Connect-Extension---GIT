package com.mirth.connect.plugins.gitplugin;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrBuilder;
import org.incava.diff.Diff;
import org.incava.diff.Difference;

public class GitVersionDiffUtility {

	public static String diffSideBySide(String fromStr, String toStr){
	    String[] fromLines = fromStr.split("\n");
	    String[] toLines = toStr.split("\n");
	    List<Difference> diffs = new Diff(fromLines, toLines).diff();
	    
	    int padding = 3;
	    int maxStrWidth = Math.max(maxLength(fromLines), maxLength(toLines)) + padding;

	    StrBuilder diffOut = new StrBuilder();
	    diffOut.setNewLineText("\n");
	    int fromLineNum = 0;
	    int toLineNum = 0;
	    for(Difference diff : diffs) {
	        int delStart = diff.getDeletedStart();
	        int delEnd = diff.getDeletedEnd();
	        int addStart = diff.getAddedStart();
	        int addEnd = diff.getAddedEnd();

	        boolean isAdd = (delEnd == Difference.NONE && addEnd != Difference.NONE);
	        boolean isDel = (addEnd == Difference.NONE && delEnd != Difference.NONE);
	        boolean isMod = (delEnd != Difference.NONE && addEnd != Difference.NONE);

	        //write out unchanged lines between diffs
	        while(true) {
	            String left = "";
	            String right = "";
	            if (fromLineNum < (delStart)){
	                left = fromLines[fromLineNum];
	                fromLineNum++;
	            }
	            if (toLineNum < (addStart)) {
	                right = toLines[toLineNum];
	                toLineNum++;
	            }
	            diffOut.append(StringUtils.rightPad(left, maxStrWidth));
	            diffOut.append("\t\t\t\t"); // no operator to display
	            diffOut.appendln(right);

	            if( (fromLineNum == (delStart)) && (toLineNum == (addStart))) {
	                break;
	            }
	        }

	        if (isDel) {
	            //write out a deletion
	            for(int i=delStart; i <= delEnd; i++) {
	                diffOut.append(StringUtils.rightPad(fromLines[i], maxStrWidth));
	                diffOut.appendln("<");
	            }
	            fromLineNum = delEnd + 1;
	        } else if (isAdd) {
	            //write out an addition
	            for(int i=addStart; i <= addEnd; i++) {
	                diffOut.append(StringUtils.rightPad("", maxStrWidth));
	                diffOut.append("> ");
	                diffOut.appendln(toLines[i]);
	            }
	            toLineNum = addEnd + 1; 
	        } else if (isMod) {
	            // write out a modification
	            while(true){
	                String left = "";
	                String right = "";
	                if (fromLineNum <= (delEnd)){
	                    left = fromLines[fromLineNum];
	                    fromLineNum++;
	                }
	                if (toLineNum <= (addEnd)) {
	                    right = toLines[toLineNum];
	                    toLineNum++;
	                }
	                diffOut.append(StringUtils.rightPad(left, maxStrWidth));
	                diffOut.append("\t\t\t\t>>>> ");
	                diffOut.appendln(right);

	                if( (fromLineNum > (delEnd)) && (toLineNum > (addEnd))) {
	                    break;
	                }
	            }
	        }

	    }

	    //we've finished displaying the diffs, now we just need to run out all the remaining unchanged lines
	    while(true) {
	        String left = "";
	        String right = "";
	        if (fromLineNum < (fromLines.length)){
	            left = fromLines[fromLineNum];
	            fromLineNum++;
	        }
	        if (toLineNum < (toLines.length)) {
	            right = toLines[toLineNum];
	            toLineNum++;
	        }
	        diffOut.append(StringUtils.rightPad(left, maxStrWidth));
	        diffOut.append("\t\t\t\t"); // no operator to display
	        diffOut.appendln(right);

	        if( (fromLineNum == (fromLines.length)) && (toLineNum == (toLines.length))) {
	            break;
	        }
	    }

	    return diffOut.toString();
	}

	private static int maxLength(String[] fromLines) {
	    int maxLength = 0;

	    for (int i = 0; i < fromLines.length; i++) {
	        if (fromLines[i].length() > maxLength) {
	            maxLength = fromLines[i].length();
	        }
	    }
	    return maxLength;
	}
}
