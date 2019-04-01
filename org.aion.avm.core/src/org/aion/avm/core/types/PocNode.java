package org.aion.avm.core.types;

import java.util.Collection;

public interface PocNode {

    public boolean isGhostNode();

    public PocClassInfo getClassInfo();

    public String getQualifiedName();

    public void addChild(PocNode node);

    public void addParent(PocNode node);

    public void removeParent(PocNode node);

    public Collection<PocNode> getParents();

    public Collection<PocNode> getChildren();

}
