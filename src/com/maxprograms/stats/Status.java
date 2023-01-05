/*******************************************************************************
 * Copyright (c) 2023 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/
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

	public Status(String source) {
		StringTokenizer tokenizer = new StringTokenizer(source, ";");
		while (tokenizer.hasMoreElements()) {
			String data = tokenizer.nextToken();
			if (data.startsWith("description")) {
				int index = data.indexOf('=');
				if (index > 0) {
					description = data.substring(index + 1);
				}
			}
			if (data.startsWith("date")) {
				int index = data.indexOf('=');
				if (index > 0) {
					date = data.substring(index + 1);
				}
			}
			if (data.startsWith("newWords")) {
				int index = data.indexOf('=');
				if (index > 0) {
					newWords = Integer.parseInt(data.substring(index + 1));
				}
			}
			if (data.startsWith("iceWords")) {
				int index = data.indexOf('=');
				if (index > 0) {
					iceWords = Integer.parseInt(data.substring(index + 1));
				}
			}
			if (data.startsWith("repeated")) {
				int index = data.indexOf('=');
				if (index > 0) {
					repeated = Integer.parseInt(data.substring(index + 1));
				}
			}
			if (data.startsWith("range0")) {
				int index = data.indexOf('=');
				if (index > 0) {
					range0Count = Integer.parseInt(data.substring(index + 1));
				}
			}
			if (data.startsWith("range1")) {
				int index = data.indexOf('=');
				if (index > 0) {
					range1Count = Integer.parseInt(data.substring(index + 1));
				}
			}
			if (data.startsWith("range2")) {
				int index = data.indexOf('=');
				if (index > 0) {
					range2Count = Integer.parseInt(data.substring(index + 1));
				}
			}
			if (data.startsWith("range3")) {
				int index = data.indexOf('=');
				if (index > 0) {
					range3Count = Integer.parseInt(data.substring(index + 1));
				}
			}
			if (data.startsWith("range4")) {
				int index = data.indexOf('=');
				if (index > 0) {
					range4Count = Integer.parseInt(data.substring(index + 1));
				}
			}
			if (data.startsWith("range4")) {
				int index = data.indexOf('=');
				if (index > 0) {
					range4Count = Integer.parseInt(data.substring(index + 1));
				}
			}
			if (data.startsWith("totalwords")) {
				int index = data.indexOf('=');
				if (index > 0) {
					totalWords = Integer.parseInt(data.substring(index + 1));
				}
			}
			if (data.startsWith("translated")) {
				int index = data.indexOf('=');
				if (index > 0) {
					translated = Integer.parseInt(data.substring(index + 1));
				}
			}
			if (data.startsWith("approved")) {
				int index = data.indexOf('=');
				if (index > 0) {
					approved = Integer.parseInt(data.substring(index + 1));
				}
			}
		}
	}

	public Status(String description, String date) {
		this.description = description;
		this.date = date;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("description=");
		result.append(description);
		result.append(';');
		result.append("date=");
		result.append(date);
		result.append(';');
		result.append("newWords=");
		result.append(newWords);
		result.append(';');
		result.append("iceWords=");
		result.append(iceWords);
		result.append(';');
		result.append("repeated=");
		result.append(repeated);
		result.append(';');
		result.append("range0=");
		result.append(range0Count);
		result.append(';');
		result.append("range1=");
		result.append(range1Count);
		result.append(';');
		result.append("range2=");
		result.append(range2Count);
		result.append(';');
		result.append("range3=");
		result.append(range3Count);
		result.append(';');
		result.append("range4=");
		result.append(range4Count);
		result.append(';');
		result.append("totalwords=");
		result.append(totalWords);
		result.append(';');
		result.append("translated=");
		result.append(translated);
		result.append(';');
		result.append("approved=");
		result.append(approved);
		return result.toString();
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
