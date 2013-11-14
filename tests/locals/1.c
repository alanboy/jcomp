int global_var;

int metodo()
{
	global_var = 123;
	return 3;
}

int main(void)
{
	int local_var;
	int b;
	int c;
	global_var = 45;
	local_var = 5;
	b = 6;
	c = 7;
	return local_var + global_var;
}

