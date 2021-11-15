#! /bin/bash

for (( i = 1 ; i <= 3 ; i++ ))  do
      if [[ "$i" -eq 3 ]]; then
            java -jar ISWC2015-CityBench.jar frequency=1 engine=csparql2 startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 query=Q5.txt duration=600s queryDuplicates=1
      else
            java -jar ISWC2015-CityBench.jar frequency=1 engine=csparql2 startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 query=Q5.txt duration=130s queryDuplicates=1
      fi
done

for (( i = 1 ; i <= 3 ; i++ ))  do
      if [[ "$i" -eq 3 ]]; then
            java -jar ISWC2015-CityBench.jar frequency=2 engine=csparql2 startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 query=Q5.txt duration=600s queryDuplicates=1
      else
            java -jar ISWC2015-CityBench.jar frequency=2 engine=csparql2 startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 query=Q5.txt duration=130s queryDuplicates=1
      fi

done

for (( i = 1 ; i <= 3 ; i++ ))  do
      if [[ "$i" -eq 3 ]]; then
            java -jar ISWC2015-CityBench.jar frequency=5 engine=csparql2 startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 query=Q5.txt duration=300s queryDuplicates=1
      else
            java -jar ISWC2015-CityBench.jar frequency=5 engine=csparql2 startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 query=Q5.txt duration=130s queryDuplicates=1
      fi
done

for (( i = 1 ; i <= 3 ; i++ ))  do
      if [[ "$i" -eq 3 ]]; then
            java -jar ISWC2015-CityBench.jar frequency=10 engine=csparql2 startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 query=Q5.txt duration=120s queryDuplicates=1
      else
            java -jar ISWC2015-CityBench.jar frequency=10 engine=csparql2 startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 query=Q5.txt duration=120s queryDuplicates=1
      fi
done

for (( i = 1 ; i <= 3 ; i++ ))  do
      if [[ "$i" -eq 3 ]]; then
            java -jar ISWC2015-CityBench.jar frequency=20 engine=csparql2 startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 query=Q5.txt duration=60s queryDuplicates=1
      else
            java -jar ISWC2015-CityBench.jar frequency=20 engine=csparql2 startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 query=Q5.txt duration=60s queryDuplicates=1
      fi
done

for (( i = 1 ; i <= 3 ; i++ ))  do
      if [[ "$i" -eq 3 ]]; then
            java -jar ISWC2015-CityBench.jar frequency=50 engine=csparql2 startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 query=Q5.txt duration=50s queryDuplicates=1
      else
            java -jar ISWC2015-CityBench.jar frequency=50 engine=csparql2 startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 query=Q5.txt duration=40s queryDuplicates=1
      fi
done