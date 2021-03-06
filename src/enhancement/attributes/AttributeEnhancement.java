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
package enhancement.attributes;

import dsa41basis.hero.Attribute;
import dsa41basis.util.DSAUtil;
import dsatool.resources.ResourceManager;
import enhancement.enhancements.Enhancement;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import jsonant.value.JSONObject;

public class AttributeEnhancement extends Enhancement {
	private final Attribute attribute;
	private final IntegerProperty start;
	private final IntegerProperty target;
	private final IntegerProperty ses;
	private final int seMin;

	private final boolean isMiserable;

	public AttributeEnhancement(Attribute attribute, JSONObject hero) {
		this.attribute = attribute;
		start = new SimpleIntegerProperty(attribute.getValue());
		target = new SimpleIntegerProperty(start.get() + 1);
		seMin = attribute.getSes();
		ses = new SimpleIntegerProperty(seMin);
		final JSONObject attributes = ResourceManager.getResource("data/Eigenschaften");
		isMiserable = hero.getObj("Nachteile").containsKey(attributes.getObj(attribute.getName()).getString("Miserable Eigenschaft"));
		fullDescription.bind(description);
		updateDescription();
		cost.set(getCalculatedCost(hero));
		recalculateValid(hero);
		cheaper.set(attribute.getSes() > 0);
	}

	@Override
	public void apply(JSONObject hero) {
		final JSONObject actual = hero.getObj("Eigenschaften").getObj(attribute.getName());
		actual.put("Wert", target.get());
		final int resultSes = ses.get() - (target.get() - start.get());
		if (resultSes <= 0) {
			actual.removeKey("SEs");
		} else {
			actual.put("SEs", resultSes);
		}
		actual.notifyListeners(null);
	}

	@Override
	public void applyTemporarily(JSONObject hero) {
		final JSONObject actual = hero.getObj("Eigenschaften").getObj(attribute.getName());
		actual.put("Wert", target.get());
	}

	@Override
	protected boolean calculateValid(JSONObject hero) {
		return target.get() <= attribute.getMaximum();
	}

	public AttributeEnhancement clone(JSONObject hero) {
		final AttributeEnhancement result = new AttributeEnhancement(attribute, hero);
		result.setTarget(target.get(), hero);
		return result;
	}

	@Override
	protected int getCalculatedCost(JSONObject hero) {
		final int SELevel = start.get() + Math.min(target.get() - start.get(), ses.get());
		return (DSAUtil.getEnhancementCost(7, start.get(), SELevel) + DSAUtil.getEnhancementCost(8, SELevel, target.get())) * (isMiserable ? 2 : 1);
	}

	@Override
	public String getName() {
		return attribute.getName();
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

	public int getTarget() {
		return target.get();
	}

	public IntegerProperty sesProperty() {
		return ses;
	}

	public void setSes(int ses, JSONObject hero) {
		this.ses.set(ses);
		resetCost(hero);
	}

	public void setTarget(int target, JSONObject hero) {
		this.target.set(target);
		updateDescription();
		recalculateValid(hero);
		resetCost(hero);
	}

	public IntegerProperty startProperty() {
		return start;
	}

	public IntegerProperty targetProperty() {
		return target;
	}

	@Override
	public void unapply(JSONObject hero) {
		final JSONObject actual = hero.getObj("Eigenschaften").getObj(attribute.getName());
		actual.put("Wert", start.get());
	}

	private void updateDescription() {
		final String desc = attribute.getName() + " (" + start.get() + "->" + target.get() + ")";
		description.set(desc);
	}
}
