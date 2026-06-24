
:meta-keywords: install, compatibility, run 
:meta-description: supported platforms, hardware and software requirements. How to install

*************
프로그램 소개
*************

본 프로그램은 CUBRID, Oracle, Tibero (RDB)에서 CoraDB (GDB)로 DB object, record 등의 정보를 마이그레이션 할 수 있도록 개발된 Eclipse RCP 기반의 도구이다.

==============
주요 기능
==============

----------------------------------------
RDB to GDB 데이터 마이그레이션
----------------------------------------

.. image:: image/MiT_structure.png
   :width: 1000px
   :height: 500px

주요 기능은 RDB(CUBRID, Oracle, Tibero)에서 table, index, fk를 추출하여 MiT 내부에서 알고리즘에 따라 총 5가지 GDB 오브젝트로 분류 후 GDB(CoraDB)로 마이그레이션하는 기능이다.

--------------
데이터 맵핑
--------------

R2G 마이그레이션 시 RDB의 데이터 타입을 GDB에 맞게 변환한다.
