package com.maxprograms.stats;

import java.util.StringTokenizer;


public class Status {
    
    private String description;
    private String date;
    private int newWords;
    private int iceWords;
    private int repeated;
    private int range0Count; // 100% match
    private int range1Count; // 95-99% match
    private int range2Count; // 85-94% match
    private int range3Count; // 75-84% match
    private int range4Count; // 50-74% match
        
    private int totalWords;
    private int translated;
    private int approved;
    
    public Status(String source){
        StringTokenizer tokenizer = new StringTokenizer(source, ";"); //$NON-NLS-1$
        while (tokenizer.hasMoreElements()){
            String data = tokenizer.nextToken();
            if (data.startsWith("description")){ //$NON-NLS-1$
                int index = data.indexOf("="); //$NON-NLS-1$
                if (index>0){
                    description = data.substring(index+1);
                }    
            }
            if (data.startsWith("date")){ //$NON-NLS-1$
                int index = data.indexOf("="); //$NON-NLS-1$
                if (index>0){
                    date = data.substring(index+1);
                }    
            }                        
            if (data.startsWith("newWords")){ //$NON-NLS-1$
                int index = data.indexOf("="); //$NON-NLS-1$
                if (index>0){
                    newWords = Integer.parseInt(data.substring(index+1));
                }    
            }
            if (data.startsWith("iceWords")){ //$NON-NLS-1$
                int index = data.indexOf("="); //$NON-NLS-1$
                if (index>0){
                	iceWords = Integer.parseInt(data.substring(index+1));
                }    
            }
            if (data.startsWith("repeated")){ //$NON-NLS-1$
                int index = data.indexOf("="); //$NON-NLS-1$
                if (index>0){
                    repeated = Integer.parseInt(data.substring(index+1));
                }    
            }
            if (data.startsWith("range0")){ //$NON-NLS-1$
                int index = data.indexOf("="); //$NON-NLS-1$
                if (index>0){
                    range0Count = Integer.parseInt(data.substring(index+1));
                }    
            }
            if (data.startsWith("range1")){ //$NON-NLS-1$
                int index = data.indexOf("="); //$NON-NLS-1$
                if (index>0){
                    range1Count = Integer.parseInt(data.substring(index+1));
                }    
            }
            if (data.startsWith("range2")){ //$NON-NLS-1$
                int index = data.indexOf("="); //$NON-NLS-1$
                if (index>0){
                    range2Count = Integer.parseInt(data.substring(index+1));
                }    
            }
            if (data.startsWith("range3")){ //$NON-NLS-1$
                int index = data.indexOf("="); //$NON-NLS-1$
                if (index>0){
                    range3Count = Integer.parseInt(data.substring(index+1));
                }    
            }
            if (data.startsWith("range4")){ //$NON-NLS-1$
                int index = data.indexOf("="); //$NON-NLS-1$
                if (index>0){
                    range4Count = Integer.parseInt(data.substring(index+1));
                }    
            }
            if (data.startsWith("range4")){ //$NON-NLS-1$
                int index = data.indexOf("="); //$NON-NLS-1$
                if (index>0){
                    range4Count = Integer.parseInt(data.substring(index+1));
                }    
            }
            if (data.startsWith("totalwords")){ //$NON-NLS-1$
                int index = data.indexOf("="); //$NON-NLS-1$
                if (index>0){
                    totalWords = Integer.parseInt(data.substring(index+1));
                }    
            }
            if (data.startsWith("translated")){ //$NON-NLS-1$
                int index = data.indexOf("="); //$NON-NLS-1$
                if (index>0){
                    translated = Integer.parseInt(data.substring(index+1));
                }    
            }
            if (data.startsWith("approved")){ //$NON-NLS-1$
                int index = data.indexOf("="); //$NON-NLS-1$
                if (index>0){
                    approved = Integer.parseInt(data.substring(index+1));
                }    
            }            
        }        
    }
    
    public Status(String description, String date){
        this.description = description;
        this.date = date;
    }
    
    @Override
	public String toString(){        
        String result = "description="+description+";"; //$NON-NLS-1$ //$NON-NLS-2$
        result = result + "date="+date+";";         //$NON-NLS-1$ //$NON-NLS-2$
        result = result + "newWords="+newWords+";"; //$NON-NLS-1$ //$NON-NLS-2$
        result = result + "iceWords="+iceWords+";"; //$NON-NLS-1$ //$NON-NLS-2$
        result = result + "repeated="+repeated+";"; //$NON-NLS-1$ //$NON-NLS-2$
        result = result + "range0="+range0Count+";";         //$NON-NLS-1$ //$NON-NLS-2$
        result = result + "range1="+range1Count+";"; //$NON-NLS-1$ //$NON-NLS-2$
        result = result + "range2="+range2Count+";"; //$NON-NLS-1$ //$NON-NLS-2$
        result = result + "range3="+range3Count+";"; //$NON-NLS-1$ //$NON-NLS-2$
        result = result + "range4="+range4Count+";";         //$NON-NLS-1$ //$NON-NLS-2$
        result = result + "totalwords="+totalWords+";"; //$NON-NLS-1$ //$NON-NLS-2$
        result = result + "translated="+translated+";";         //$NON-NLS-1$ //$NON-NLS-2$
        result = result + "approved="+approved;         //$NON-NLS-1$
        return result;
    }    
    
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public int getNewWords() {
        return newWords;
    }
    public void setNewWords(int newWords) {
        this.newWords = newWords;
    }
    public int getRange0Count() {
        return range0Count;
    }
    public void setRange0Count(int range0Count) {
        this.range0Count = range0Count;
    }
    public int getRange1Count() {
        return range1Count;
    }
    public void setRange1Count(int range1Count) {
        this.range1Count = range1Count;
    }
    public int getRange2Count() {
        return range2Count;
    }
    public void setRange2Count(int range2Count) {
        this.range2Count = range2Count;
    }
    public int getRange3Count() {
        return range3Count;
    }
    public void setRange3Count(int range3Count) {
        this.range3Count = range3Count;
    }
    public int getRange4Count() {
        return range4Count;
    }
    public void setRange4Count(int range4Count) {
        this.range4Count = range4Count;
    }
    public int getRepeated() {
        return repeated;
    }
    public void setRepeated(int repeated) {
        this.repeated = repeated;
    }
    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }
    public int getApproved() {
        return approved;
    }
    public void setApproved(int approved) {
        this.approved = approved;
    }
    public int getTotalWords() {
        return totalWords;
    }
    public void setTotalWords(int totalWords) {
        this.totalWords = totalWords;
    }
    public int getTranslated() {
        return translated;
    }
    public void setTranslated(int translated) {
        this.translated = translated;
    }

	public int getIceWords() {
		return iceWords;
	}

	public void setIceWords(int value) {
		iceWords = value;
	}

}
