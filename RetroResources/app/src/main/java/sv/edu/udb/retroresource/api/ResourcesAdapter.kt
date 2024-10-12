package sv.edu.udb.retroresource.api

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import sv.edu.udb.retroresource.MainActivity
import sv.edu.udb.retroresource.R
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class ResourcesAdapter(
    private val context: Context, // Agregar contexto
    private val recursos: List<Resources>,
    private val api: ResourcesApi, // Añadir la API aquí
    private val onItemClick: (Resources) -> Unit // Listener para clic en el recurso
) : RecyclerView.Adapter<ResourcesAdapter.ViewHolder>() {

    // Lista que contendrá los recursos filtrados
    private var recursosFiltrados: MutableList<Resources> = recursos.toMutableList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombreTextView: TextView = view.findViewById(R.id.tvNombre)
        val descripcionView: TextView = view.findViewById(R.id.tvDescripcion)
        val tipoTextView: TextView = view.findViewById(R.id.tvTipo) // Nuevo campo para tipo
        val imagenView: ImageView = view.findViewById(R.id.ivImagen) // Nuevo ImageView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recurso_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recurso = recursosFiltrados[position]
        holder.nombreTextView.text = recurso.titulo
        holder.descripcionView.text = recurso.descripcion
        holder.tipoTextView.text = recurso.tipo // Mostrar el tipo de recurso

        // Cargar la imagen de manera nativa
        val imageUrl = recurso.imagen // URL de la imagen
        loadImageFromUrl(imageUrl, holder.imagenView)

        // Manejar el clic en el elemento
        holder.itemView.setOnClickListener {
            onItemClick(recurso) // Llama al listener pasando el recurso
        }

        holder.itemView.setOnLongClickListener {
            // Mostrar el cuadro de diálogo
            mostrarDialogoOpciones(recurso, position)
            true // Indica que el evento fue manejado
        }
    }

    override fun getItemCount(): Int {
        return recursosFiltrados.size
    }

    // Método para cargar la imagen desde la URL nativamente sin Glide
    private fun loadImageFromUrl(imageUrl: String, imageView: ImageView) {
        thread {
            try {
                val url = URL(imageUrl)
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val inputStream = connection.inputStream
                val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)

                // Actualizar el ImageView en el hilo principal
                (context as MainActivity).runOnUiThread {
                    imageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Método para filtrar la lista
    fun filtrar(query: String) {
        recursosFiltrados = if (query.isEmpty()) {
            recursos.toMutableList()
        } else {
            recursos.filter {
                it.titulo.contains(query, ignoreCase = true) ||
                        it.enlace.contains(query, ignoreCase = true) ||
                        it.descripcion.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    private fun mostrarDialogoOpciones(recurso: Resources, position: Int) {
        AlertDialog.Builder(context) // Asegúrate de tener acceso al contexto
            .setTitle("Opciones")
            .setMessage("¿Qué deseas hacer con el recurso: ${recurso.titulo}?")
            .setPositiveButton("Modificar") { dialog, which ->
                modificarRecurso(recurso, position) // Llama al método de modificación


            }
            .setNegativeButton("Eliminar") { dialog, which ->
                eliminarRecurso(position) // Llama al método de eliminación
                Toast.makeText(context, "Recurso ${recurso.titulo} eliminado correctamente", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Cancelar", null) // Opción para cancelar
            .show()
    }

    fun eliminarRecurso(position: Int) {
        val recurso = recursosFiltrados[position]

        // Llamar a la API para eliminar el recurso
        val call = api.eliminarRecurso(recurso.id.toString())
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    // Eliminar el recurso de la lista
                    recursosFiltrados.removeAt(position)
                    // Notificar al adaptador que los datos han cambiado
                    notifyItemRemoved(position)
                } else {
                    Log.e("API", "Error al eliminar el recurso: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("API", "Error al eliminar el recurso: ${t.message}")
            }
        })
    }

    private fun isValidUrl(url: String): Boolean {
        // Patrón de URL que comienza con http:// o https://
        val urlPattern = "^(https?://)([a-zA-Z0-9\\-]+\\.)+[a-zA-Z]{2,}(/\\S*)?$"
        return url.matches(Regex(urlPattern))
    }

    private fun modificarRecurso(recurso: Resources, position: Int) {
        // Mostrar un formulario para editar el recurso
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_editar_recurso, null)
        builder.setView(dialogView)

        val editTextNombre = dialogView.findViewById<EditText>(R.id.editTextNombre)
        val editTextRecursoEnlace = dialogView.findViewById<EditText>(R.id.editTextRecursoEnlace) // URL del recurso
        val editTextImagenEnlace = dialogView.findViewById<EditText>(R.id.editTextImagenEnlace) // URL de la imagen
        val editTextDescripcion = dialogView.findViewById<EditText>(R.id.editTextDescripcion)
        val editTextTipo = dialogView.findViewById<EditText>(R.id.editTextTipo) // Campo para el tipo

        // Pre-llenar con los datos actuales
        editTextNombre.setText(recurso.titulo)
        editTextRecursoEnlace.setText(recurso.enlace) // URL del recurso
        editTextImagenEnlace.setText(recurso.imagen) // URL de la imagen
        editTextDescripcion.setText(recurso.descripcion)
        editTextTipo.setText(recurso.tipo) // Pre-llenar con el tipo actual

        builder.setTitle("Modificar Recurso")
            .setPositiveButton("Guardar") { dialog, which ->
                // Recoger los nuevos datos
                val nuevoNombre = editTextNombre.text.toString().trim()
                val nuevoRecursoEnlace = editTextRecursoEnlace.text.toString().trim() // URL del recurso
                val nuevoImagenEnlace = editTextImagenEnlace.text.toString().trim() // URL de la imagen
                val nuevaDescripcion = editTextDescripcion.text.toString().trim()
                val nuevoTipo = editTextTipo.text.toString().trim()

                // Validar que los campos no estén vacíos y que las URLs sean válidas
                if (nuevoNombre.isNotEmpty() && nuevoRecursoEnlace.isNotEmpty() && isValidUrl(nuevoRecursoEnlace) &&
                    nuevoImagenEnlace.isNotEmpty() && isValidUrl(nuevoImagenEnlace) && nuevaDescripcion.isNotEmpty() && nuevoTipo.isNotEmpty()) {

                    // Llamar al método de modificación en la API
                    val nuevoRecurso = recurso.copy(titulo = nuevoNombre, enlace = nuevoRecursoEnlace, imagen = nuevoImagenEnlace, descripcion = nuevaDescripcion, tipo = nuevoTipo)
                    actualizarRecurso(nuevoRecurso, position)
                } else {
                    // Mostrar un mensaje de error si la validación falla
                    Toast.makeText(context, "Por favor, completa todos los campos y verifica las URLs", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    private fun actualizarRecurso(nuevoRecurso: Resources, position: Int) {
        nuevoRecurso.id?.let { recursoId ->
            api.actualizarRecurso(recursoId, nuevoRecurso).enqueue(object : Callback<Resources> {
                override fun onResponse(call: Call<Resources>, response: Response<Resources>) {
                    if (response.isSuccessful) {
                        recursosFiltrados[position] = response.body() ?: nuevoRecurso
                        notifyItemChanged(position)
                        Toast.makeText(context, "Recurso ${nuevoRecurso.titulo} modificado correctamente", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error al actualizar el recurso", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Resources>, t: Throwable) {
                    Toast.makeText(context, "Error de conexión: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } ?: run {
            Toast.makeText(context, "ID de recurso es nulo", Toast.LENGTH_SHORT).show()
        }
    }


    // Método para limpiar los datos filtrados
    fun clearDatos() {
        recursosFiltrados.clear() // Limpia la lista de datos filtrados
        notifyDataSetChanged() // Notifica al adaptador que los datos han cambiado
    }
}

