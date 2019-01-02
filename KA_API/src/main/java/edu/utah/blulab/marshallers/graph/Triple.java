package edu.utah.blulab.marshallers.graph;

import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Relationship;

public class Triple {

    Relationship rel;
    Node node1;
    Node node2;
    Direction direction;

    enum Direction {
        LEFT, RIGHT, BOTH, NONE;
    }


    public Relationship getRel() {
        return rel;
    }

    public void setRel(Relationship rel) {
        this.rel = rel;
    }

    public Node getNode1() {
        return node1;
    }

    public void setNode1(Node node1) {
        this.node1 = node1;
    }

    public Node getNode2() {
        return node2;
    }

    public void setNode2(Node node2) {
        this.node2 = node2;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public boolean isEqualReverse(Triple trip2){
        Direction reverseDirect;
        if (direction == Direction.RIGHT){
            reverseDirect = Direction.LEFT;
        } else if (direction == Direction.LEFT){
            reverseDirect = Direction.RIGHT;
        } else {
            reverseDirect = Direction.BOTH;
        }

        if (trip2.rel.equals(rel) &
                trip2.node1.equals(node2) &
                trip2.node2.equals(node1) &
                trip2.direction.equals(reverseDirect) ) {
            return true;
        }else {
            return false;
        }
    }
    public boolean isEqual(Triple trip2){
        if (trip2.rel.equals(rel) &
            trip2.node1.equals(node1) &
               trip2.node2.equals(node2) &
               trip2.direction.equals(direction) ){
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Triple{" +
                "rel=" + rel +
                ", Node1=" + node1 +
                ", Node2=" + node2 +
                ", direction=" + direction +
                '}';
    }

    public String toStringReverse() {
        Direction reverseDirect;
        if (direction == Direction.RIGHT){
            reverseDirect = Direction.LEFT;
        } else if (direction == Direction.LEFT){
            reverseDirect = Direction.RIGHT;
        } else {
            reverseDirect = Direction.BOTH;
        }
        return "Triple{" +
                "rel=" + rel +
                ", Node1=" + node2 +
                ", Node2=" + node1 +
                ", direction=" + reverseDirect +
                '}';
    }
}
