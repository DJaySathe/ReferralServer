package Util;

import models.Neighbor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Stack;

public class ReferralChain {
    public static int index=0;
    public static Stack<Neighbor> next=new Stack<>();
    public static LinkedHashSet<String> refChain=new LinkedHashSet<>();
}
