package com.morphengine.nexus.client.screen;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.menu.AccessView;
import com.morphengine.nexus.security.EditResult;
import com.morphengine.nexus.security.Editor;
import com.morphengine.nexus.security.NetworkSecurity;
import com.morphengine.nexus.security.SecurityEdit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * What the viewer of the access tab may change, by asking the rules the
 * server sent what each edit would come to, the same rules the server then
 * applies. One per view received; answers are kept, as the panel asks the
 * same questions every frame.
 */
final class AccessChoices {

    private final AccessView view;
    private final NetworkSecurity rules;
    private final Editor editor;
    private final Map<SecurityEdit, Boolean> answers = new HashMap<>();

    AccessChoices(final AccessView view, final UUID viewer) {
        this.view = view;
        this.rules = view.rules();
        this.editor = view.editor(viewer);
    }

    AccessView view() {
        return view;
    }

    /**
     * @return the rules as sent, for reading; never changed
     */
    NetworkSecurity rules() {
        return rules;
    }

    boolean allows(final SecurityEdit edit) {
        return answers.computeIfAbsent(edit, asked -> rules.apply(editor, asked, Action.SIMULATE)
                == EditResult.APPLIED);
    }
}
