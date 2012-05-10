(ns taxidermy.forms
  (:require [taxidermy.fields :as fields])
  (:import [taxidermy.fields Field]))

(defprotocol BaseForm
  (field [this field-name]))

(defrecord Form [name]
  BaseForm
  (field [this field-name]
    (merge (:widget ((keyword field-name) (:fields this))))))

(defn- coerce-values
  [field-keys coercions values]
  (for [field-key field-keys]
    (let [coercion (field-key coercions)]
      (coercion (field-key values)))))

(defn- coerce-form-values
  [form values]
  (let [fields (:fields form)
        field-keys (keys fields)
        field-vals (map #(% fields) field-keys)
        coercions (zipmap field-keys (map #(fields/coercion-partial %) field-vals))
        coerced-values (coerce-values field-keys coercions values)]
    (zipmap field-keys coerced-values)))

(defn make-form [form-name values & {:keys [fields] :as options}]
  (let [form (Form. form-name)]
    (merge form
      (hash-map :values values :fields
        (let [processed-fields
                (for [field fields]
                  (let [field-value ((keyword (:field-name field)) values)]
                    (merge field {:data field-value})))]
          (zipmap (map #(keyword (:field-name %)) fields) processed-fields))))))

(defn validate-field
  [form-values field]
  (let [validators (:validators field)]
    (filter (comp not nil?) (map #(% form-values (:value field)) validators))))

(defn validate
  "Validate each field in the form.  Returns a map with field names as keys and lists of
   errors as values"
  [form]
  (let [form-values (:values form)
        fields (:fields form)]
    (reduce (fn [acc field-map] (assoc acc (key field-map) (validate-field form-values (val field-map)))) {} fields)))

(defmacro field-validator [validation-func error-func]
  `(fn [form-values# field-value#]
    (if (not (~validation-func form-values# field-value#)) (~error-func))))

(defmacro defform [form-name & options]
  `(defn ~form-name [values#]
    (make-form ~(keyword form-name) values# ~@options)))

;(defform contact-form
  ;(text-field :field-name "firstname" :validators [my-val)
  ;(text-field :field-name "lastname"))

;(defn controller-action
  ;[request]
  ;(let [base-contact-f (contact-form (request :form-values))
        ;contact-f (-> contact-form
                      ;(override-widget :firstname TextArea)
                      ;(override-widget :lastname TextArea))]
    ;(.validate (contact-form))
    ;(render (merge request {:contact_form contact-f}))))

;<html>
;<form>
;<#list contact_form.errors as error>
  ;<p>${error}</p>
;</#list>
  ;${contact_form.element("firstname", class="class1 class1")}
;</form>


;{:name "contact form"
 ;:elements {
             ;:firstname {:name "firstname"
              ;:type Boolean
              ;:widget Checkbox
              ;:value "Ryan"
              ;:validators []}
             ;:lastname {:name "lastname"
              ;:widget Text
              ;:value "roemmich"
              ;:validators []
            ;}
 ;:validators []
;}
