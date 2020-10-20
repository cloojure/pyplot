(ns tst.demo.core
  (:use demo.core tupelo.core tupelo.test)
  (:require
    [libpython-clj.python :as py]
    [libpython-clj.python :refer [py. py.. py.-]]
    [libpython-clj.require :refer [require-python]]

    [clojure.java.shell :as sh]
    [clojure.string :as str]
    [tupelo.misc :as misc]
    )
  (:import [java.io File]))


(py/initialize!)
(comment
  ; No need to manually configure paths if using pip3 for Python config
  (py/initialize! ; Ubuntu 20.04 exampel with Python 3.8
    :python-executable "/usr/bin/python3.8"
    :library-path "/usr/lib/python3.8/config-3.8-x86_64-linux-gnu/libpython3.8.so"))

(require-python '[numpy :as np])

;---------------------------------------------------------------------------------------------------
(dotest
  (spyx-pretty (np/array [[1 2] [3 4]]))
  (let [x (spyx (np/arange 6))
        y (spyx-pretty (np/reshape x [2 3]))
        z (np/arange 10)]
    (spyx-pretty y)
    (spyx (.shape y))

    (throws? (.dtype y))
    (throws? (.-dtype y))
    (spyx (py/get-attr y "dtype"))
    (spyx (py.- y "ndim")) ; object getter syntax
    (spyx (py.- y "shape")) ; object getter syntax
    (spyx (py.- y "size")) ; object getter syntax
    (spyx (py.- y "dtype")) ; object getter syntax
    (spyx (py.- y "itemsize")) ; object getter syntax

    )
  (spyx-pretty (-> (np/arange 6) (np/reshape [2 3])))

  (nl)
  (spyx-pretty (py/run-simple-string "(2 + 3)"))

  )

;---------------------------------------------------------------------------------------------------
(def system-display-cmd
  (condp = (misc/get-os)
    :mac "open"
    :linux "display"
    :else (throw (ex-info "invalid O.S. found"))))

(defn display-image
  "Display image on OSX or on Linux based system"
  [image-file]
  (println "display-image - enter")
  (sh/sh system-display-cmd image-file)
  (println "display-image - leave"))

(defn create-tmp-file
  "Return full path of temporary file.

  Example:
  (create-tmp-file \"tmp-image\" \".png\") "
  [prefix ext]
  (File/createTempFile prefix ext))

;---------------------------------------------------------------------------------------------------
; have to set the headless mode before requiring pyplot
(def mplt (py/import-module "matplotlib"))
(py. mplt "use" "Agg")

(require-python '[matplotlib.pyplot :as pyplot])
(require-python 'matplotlib.backends.backend_agg)
(require-python 'numpy)

(defmacro with-show
  "Takes forms with mathplotlib.pyplot to then show locally"
  [& body]
  `(let [_# (pyplot/clf)
         fig# (pyplot/figure)
         agg-canvas# (matplotlib.backends.backend_agg/FigureCanvasAgg fig#)
         temp-file# (create-tmp-file "tmp-image" ".png")
         temp-image# (.getAbsolutePath temp-file#)]
     ~(cons 'do body)
     (py. agg-canvas# "draw")
     (pyplot/savefig temp-image#)
     (.deleteOnExit temp-file#)
     ; avoid blocking thread - O.S. call from display-image blocks until user closes graphics window
     (future
       (display-image temp-image#))))

(dotest
  (with-show (pyplot/plot [[1 2 3 4 5] [1 2 3 4 10]] :label "linear")))

