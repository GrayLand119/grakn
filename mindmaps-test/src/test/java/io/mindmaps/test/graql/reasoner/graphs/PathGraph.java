/*
 * MindmapsDB - A Distributed Semantic Database
 * Copyright (C) 2016  Mindmaps Research Ltd
 *
 * MindmapsDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MindmapsDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MindmapsDB. If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package io.mindmaps.test.graql.reasoner.graphs;

import io.mindmaps.MindmapsGraph;
import io.mindmaps.concept.EntityType;
import io.mindmaps.concept.Instance;
import io.mindmaps.concept.RelationType;
import io.mindmaps.concept.Resource;
import io.mindmaps.concept.ResourceType;
import io.mindmaps.concept.RoleType;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.math.IntMath.pow;

public class PathGraph extends GenericGraph {

    public static MindmapsGraph getGraph(int n, int children) {
        final String gqlFile = "path-test.gql";
        getGraph(gqlFile);
        buildExtensionalDB(n, children);
        commit();
        return mindmaps;
    }

    protected static void buildExtensionalDB(int n, int children) {
        long startTime = System.currentTimeMillis();

        EntityType vertex = mindmaps.getEntityType("vertex");
        EntityType startVertex = mindmaps.getEntityType("start-vertex");
        RoleType arcFrom = mindmaps.getRoleType("arc-from");
        RoleType arcTo = mindmaps.getRoleType("arc-to");

        RelationType arc = mindmaps.getRelationType("arc");
        putEntity("a0", startVertex);

        for(int i = 1 ; i <= n ;i++) {
            int m = pow(children, i);
            for (int j = 0; j < m; j++) {
                putEntity("a" + i + "," + j, vertex);
                if (j != 0 && j % 100 ==0)
                    System.out.println(j + " entities out of " + m + " inserted");
            }
        }

        for (int j = 0; j < children; j++) {
            mindmaps.addRelation(arc)
                    .putRolePlayer(arcFrom, getInstance("a0"))
                    .putRolePlayer(arcTo, getInstance("a1," + j));
        }

        for(int i = 1 ; i < n ;i++) {
            int m = pow(children, i);
            for (int j = 0; j < m; j++) {
                for (int c = 0; c < children; c++) {
                    mindmaps.addRelation(arc)
                            .putRolePlayer(arcFrom, getInstance("a" + i + "," + j))
                            .putRolePlayer(arcTo, getInstance("a" + (i + 1) + "," + (j * children + c)));

                }
                if (j!= 0 && j % 100 == 0)
                    System.out.println("level " + i + "/" + (n-1) + ": " + j + " entities out of " + m + " connected");
            }
        }

        long loadTime = System.currentTimeMillis() - startTime;
        System.out.println("PathGraph loading time: " + loadTime + " ms");
    }

    private static Instance getInstance(String id){
        Set<Instance> instances = mindmaps.getResourcesByValue(id)
                .stream().flatMap(res -> res.ownerInstances().stream()).collect(Collectors.toSet());
        if (instances.size() != 1)
            throw new IllegalStateException("Something wrong, multiple instances with given res value");
        return instances.iterator().next();
    }

    private static Instance putEntity(String id, EntityType type) {
        ResourceType<String> index = mindmaps.getResourceType("index");
        RelationType indexRelation = mindmaps.getRelationType("has-index");
        RoleType indexTarget = mindmaps.getRoleType("has-index-owner");
        RoleType indexValue = mindmaps.getRoleType("has-index-value");
        Instance inst = mindmaps.addEntity(type);
        putResource(inst, index, id, indexRelation, indexTarget, indexValue);
        return inst;
    }

    private static <T> void putResource(Instance instance, ResourceType<T> resourceType, T resource, RelationType relationType,
                                        RoleType targetRole, RoleType valueRole) {
        Resource resourceInstance = mindmaps.putResource(resource, resourceType);
        mindmaps.addRelation(relationType)
                .putRolePlayer(targetRole, instance)
                .putRolePlayer(valueRole, resourceInstance);
    }
}