package com.sourabhsrivastava.entity;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaInterview {

	public static void main(String[] args) {

		List<Integer> list1 = List.of(1, 3, 5, 7, 2);
		List<Integer> list2 = List.of(2, 4, 6, 8,3,5);
	
		
		List<Integer> rsult2= Stream.concat(list1.stream()
				                   .filter(n -> !list2.contains(n)),
				                    list2.stream()
				                    .filter( n -> !list1.contains(n)))
				                  .collect(Collectors.toList());

				                   System.out.println(rsult2);
				
				//Stream.concat(null, null)
	}

}
