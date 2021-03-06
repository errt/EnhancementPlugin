/*
 * Copyright 2017 DSATool team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package enhancement.talents;

import java.util.Collection;
import java.util.Stack;

import dsa41basis.hero.Talent;
import dsa41basis.util.DSAUtil;
import dsa41basis.util.RequirementsUtil;
import dsatool.resources.Settings;
import enhancement.enhancements.Enhancement;
import enhancement.enhancements.EnhancementController;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class TalentEnhancement extends Enhancement {
	protected final Talent talent;
	protected final IntegerProperty start;
	protected final StringProperty startString;
	protected final IntegerProperty target;
	protected final StringProperty targetString;
	protected boolean basis;
	protected final String talentGroupName;
	protected StringProperty method;
	protected IntegerProperty ses;
	protected int seMin;
	protected boolean temporary;

	private final ChangeListener<Boolean> chargenListener;

	public TalentEnhancement(final Talent talent, final String talentGroupName, final JSONObject hero) {
		this.talent = talent;
		this.talentGroupName = talentGroupName;
		basis = talent.getTalent().getBoolOrDefault("Basis", false);
		int value = talent.getValue();
		if (value == Integer.MIN_VALUE) {
			value = -1;
		} else if (value < 0 && !basis) {
			value -= 1;
		}
		startString = new SimpleStringProperty(getOfficial(value));
		start = new SimpleIntegerProperty(value);
		target = new SimpleIntegerProperty(value + 1);
		targetString = new SimpleStringProperty(getOfficial(value + 1));
		seMin = talent.getSes();
		ses = new SimpleIntegerProperty(seMin);
		fullDescription.bind(description);
		method = new SimpleStringProperty(Settings.getSettingStringOrDefault("Gegenseitiges Lehren", "Steigerung", "Lernmethode"));
		updateDescription();

		final Stack<Enhancement> enhancements = new Stack<>();
		for (final Enhancement e : EnhancementController.instance.getEnhancements()) {
			e.applyTemporarily(hero);
			enhancements.push(e);
		}
		cost.set(getCalculatedCost(hero));
		recalculateValid(hero);
		for (final Enhancement e : enhancements) {
			e.unapply(hero);
		}

		cheaper.bind(ses.greaterThan(0));
		temporary = talent.getActual() == null;
		chargenListener = (o, oldV, newV) -> resetCost(hero);
		EnhancementController.usesChargenRules.addListener(chargenListener);
	}

	@Override
	public void apply(final JSONObject hero) {
		final int resultSes = Math.max(ses.get() - (target.get() - start.get()), 0);
		talent.insertTalent(true);
		talent.setValue(target.get());
		talent.setSes(resultSes);
	}

	@Override
	public void applyTemporarily(final JSONObject hero) {
		talent.insertTalent(true);
		talent.setValue(target.get());
	}

	@Override
	protected boolean calculateValid(final JSONObject hero) {
		final JSONObject talent = this.talent.getTalent();
		boolean valid = target.get() <= this.talent.getMaximum(hero);
		if (!talent.containsKey("Voraussetzungen")) return valid;
		final JSONArray requirements = talent.getArr("Voraussetzungen");
		for (int i = 0; i < requirements.size(); ++i) {
			final JSONObject requirement = requirements.getObj(i);
			if (!requirement.containsKey("Ab") || target.get() > requirement.getInt("Ab")) {
				valid = valid && RequirementsUtil.isRequirementFulfilled(hero, requirement, null, null, false);
			}
		}
		return valid;
	}

	public TalentEnhancement clone(final JSONObject hero, final Collection<Enhancement> enhancements) {
		final TalentEnhancement result = new TalentEnhancement(talent, talentGroupName, hero);
		result.start.set(start.get());
		result.startString.set(startString.get());
		result.target.set(target.get());
		result.targetString.set(targetString.get());
		result.basis = basis;
		result.method = method;
		result.ses = ses;
		result.seMin = seMin;
		result.temporary = temporary;
		return result;
	}

	private int fromOfficial(final String taw) {
		if ("n.a.".equals(taw)) return -1;
		int value = Integer.parseInt(taw);
		if (value < 0 && !basis) {
			value -= 1;
		}
		return value;
	}

	@Override
	public int getCalculatedCost(final JSONObject hero) {
		int cost = 0;
		final String method = EnhancementController.usesChargenRules.get() ? "Gegenseitiges Lehren" : this.method.get();

		final int SELevel = start.get() + Math.min(target.get() - start.get(), ses.get());
		cost += DSAUtil.getEnhancementCost(talent, hero, "Lehrmeister", start.get(), Math.max(SELevel, start.get()),
				EnhancementController.usesChargenRules.get());
		cost += DSAUtil.getEnhancementCost(talent, hero, method, SELevel, Math.max(target.get(), SELevel),
				EnhancementController.usesChargenRules.get());
		return cost;
	}

	public String getMethod() {
		return method.get();
	}

	@Override
	public String getName() {
		return talent.getName();
	}

	protected String getOfficial(final int taw) {
		if (taw == -1)
			return "n.a.";
		else if (taw < -1 && !basis)
			return String.valueOf(taw + 1);
		else
			return String.valueOf(taw);
	}

	public int getSeMin() {
		return seMin;
	}

	public int getSes() {
		return ses.get();
	}

	public int getStart() {
		return start.get();
	}

	public Talent getTalent() {
		return talent;
	}

	public int getTarget() {
		return target.get();
	}

	public boolean isBasis() {
		return basis;
	}

	public StringProperty methodProperty() {
		return method;
	}

	public IntegerProperty sesProperty() {
		return ses;
	}

	public void setMethod(final String method, final JSONObject hero) {
		this.method.set(method);
		resetCost(hero);
	}

	public void setSes(final int ses, final JSONObject hero) {
		this.ses.set(ses);
		resetCost(hero);
	}

	public void setTarget(final int target, final JSONObject hero, final Collection<Enhancement> enhancements) {
		final Stack<Enhancement> enhancementStack = new Stack<>();
		for (final Enhancement e : enhancements) {
			e.applyTemporarily(hero);
			enhancementStack.push(e);
		}

		this.target.set(target);
		targetString.set(getOfficial(target));
		updateDescription();
		recalculateValid(hero);
		resetCost(hero);

		for (final Enhancement e : enhancementStack) {
			e.unapply(hero);
		}
	}

	public void setTarget(final String newValue, final JSONObject hero, final Collection<Enhancement> enhancements) {
		setTarget(fromOfficial(newValue), hero, enhancements);
	}

	public IntegerProperty startProperty() {
		return start;
	}

	public ReadOnlyStringProperty startStringProperty() {
		return startString;
	}

	public IntegerProperty targetProperty() {
		return target;
	}

	public ReadOnlyStringProperty targetStringProperty() {
		return targetString;
	}

	@Override
	public void unapply(final JSONObject hero) {
		talent.setValue(start.get());
		if (temporary) {
			talent.removeTalent();
		}
	}

	public void unregister() {
		EnhancementController.usesChargenRules.removeListener(chargenListener);
	}

	protected void updateDescription() {
		final String desc = talent.getDisplayName() + " (" + startString.get() + "->" + targetString.get() + ")";
		description.set(desc);
	}
}
